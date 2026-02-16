package com.storage.app.dto.resource.response;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Component
@Data
public class DownloadResourceDto {
    private StreamingResponseBody streamingResponseBody;
    private String fileExtension;
    private String fileName;
}
