
package com.xhxi.photobooker.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ServiceData implements Serializable {
    private String serviceType;
    private String expectedTime;
    private String refundReason;
    private String complaintType;
    private List<String> additionalItems;
    private String description;
    private String contactPhone;
    private String[] evidenceImages;
}

