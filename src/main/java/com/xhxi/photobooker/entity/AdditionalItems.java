// 新建文件：src/main/java/com/xhxi/photobooker/entity/AdditionalItems.java
package com.xhxi.photobooker.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class AdditionalItems implements Serializable {
    private Boolean extraRefine;      // 额外精修
    private Boolean photoAlbum;       // 定制相册
    private Boolean photoFrame;       // 精美相框
    private Boolean allPhotos;        // 全底片赠送
    private Integer refineCount;      // 精修数量
}
