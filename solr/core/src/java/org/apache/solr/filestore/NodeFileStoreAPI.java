/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.filestore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.solr.handler.ReplicationHandler.FILE_STREAM;

import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.solr.api.JerseyResource;
import org.apache.solr.client.api.endpoint.NodeFileStoreApis;
import org.apache.solr.client.api.model.FileStoreFileMetadata;
import org.apache.solr.client.api.model.ReadFileStoreResponse;
import org.apache.solr.client.api.model.SolrJerseyResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.jersey.PermissionName;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.security.PermissionNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFileStoreAPI extends JerseyResource implements NodeFileStoreApis {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final CoreContainer coreContainer;
  private final SolrQueryRequest req;
  private final SolrQueryResponse rsp;
  private final FileStore fileStore;

  @Inject
  public NodeFileStoreAPI(
      CoreContainer coreContainer,
      DistribFileStore fileStore,
      SolrQueryRequest req,
      SolrQueryResponse rsp) {
    this.coreContainer = coreContainer;
    this.req = req;
    this.rsp = rsp;
    this.fileStore = fileStore;
  }

  @Override
  @PermissionName(PermissionNameProvider.Name.FILESTORE_READ_PERM)
  public ReadFileStoreResponse readFileStoreEntry(
      String path, Boolean sync, String getFrom, Boolean meta) {
    final var response = instantiateJerseyResponse(ReadFileStoreResponse.class);
    log.info("JEGERLOW: Path is: {}", path);
    String pathCopy = path;
    if (BooleanUtils.isTrue(sync)) {
      try {
        fileStore.syncToAllNodes(path);
        return response;
      } catch (IOException e) {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Error getting file ", e);
      }
    }
    if (getFrom != null) {
      coreContainer
          .getUpdateShardHandler()
          .getUpdateExecutor()
          .submit(
              () -> {
                log.debug("Downloading file {}", pathCopy);
                try {
                  fileStore.fetch(pathCopy, getFrom);
                } catch (Exception e) {
                  log.error("Failed to download file: {}", pathCopy, e);
                }
                log.info("downloaded file: {}", pathCopy);
              });
      return response;
    }
    if (path == null) {
      path = "";
    }

    FileStore.FileType typ = fileStore.getType(path, false);
    if (typ == FileStore.FileType.NOFILE) {
      response.files = Collections.singletonMap(path, null);
      return response;
    } else if (typ == FileStore.FileType.DIRECTORY) {
      response.files = Collections.singletonMap(path, fileStore.list(path, null));
      return response;
    }

    if (BooleanUtils.isTrue(meta)) {
      if (typ == FileStore.FileType.FILE) {
        int idx = path.lastIndexOf('/');
        String fileName = path.substring(idx + 1);
        String parentPath = path.substring(0, path.lastIndexOf('/'));
        List<FileStore.FileDetails> l = fileStore.list(parentPath, s -> s.equals(fileName));
        final var fileMetaVal = l.isEmpty() ? null : convertToResponseMetadata(l.get(0));
        response.files = Collections.singletonMap(path, fileMetaVal);
        return response;
      }
    } else {
      writeRawFile(req, rsp, path);
    }

    return response;
  }

  private FileStoreFileMetadata convertToResponseMetadata(FileStore.FileDetails details) {
    final var response = new FileStoreFileMetadata();
    response.name = details.getSimpleName();
    response.size = details.size();
    response.timestamp = details.getTimeStamp();
    final var metadata = details.getMetaData();
    if (metadata != null) {
      response.sha512 = metadata.sha512;
      response.sig = metadata.signatures;
      response.extraProperties = metadata.otherAttribs;
    }
    return response;
  }

  @Override
  @PermissionName(PermissionNameProvider.Name.FILESTORE_WRITE_PERM)
  public SolrJerseyResponse deleteFileLocal(String path) {
    FileStoreUtils.validateName(path, true);
    fileStore.deleteLocal(path);
    return new SolrJerseyResponse();
  }

  private void writeRawFile(SolrQueryRequest req, SolrQueryResponse rsp, String path) {
    ModifiableSolrParams solrParams = new ModifiableSolrParams();
    if ("json".equals(req.getParams().get(CommonParams.WT))) {
      solrParams.add(CommonParams.WT, "json");
      req.setParams(SolrParams.wrapDefaults(solrParams, req.getParams()));
      try {
        fileStore.get(
            path,
            it -> {
              try {
                InputStream inputStream = it.getInputStream();
                if (inputStream != null) {
                  rsp.addResponse(new String(inputStream.readAllBytes(), UTF_8));
                }
              } catch (IOException e) {
                throw new SolrException(
                    SolrException.ErrorCode.SERVER_ERROR, "Error reading file " + path);
              }
            },
            false);
      } catch (IOException e) {
        throw new SolrException(
            SolrException.ErrorCode.SERVER_ERROR, "Error getting file from path " + path);
      }
    } else {
      solrParams.add(CommonParams.WT, FILE_STREAM);
      req.setParams(SolrParams.wrapDefaults(solrParams, req.getParams()));
      rsp.add(
          FILE_STREAM,
          (SolrCore.RawWriter)
              os ->
                  fileStore.get(
                      path,
                      it -> {
                        try {
                          InputStream inputStream = it.getInputStream();
                          if (inputStream != null) {
                            inputStream.transferTo(os);
                          }
                        } catch (IOException e) {
                          throw new SolrException(
                              SolrException.ErrorCode.SERVER_ERROR, "Error reading file " + path);
                        }
                      },
                      false));
    }
  }
}
