package com.sdjzu.carrental.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MediaFileVO {

    private String url;
    private String fileName;
    private long size;
}
