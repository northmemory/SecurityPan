package com.xpb.entities.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class FileUploadResultDto implements Serializable {
   private String fileId;
   private Integer status;
   private String statusDescription;

}
