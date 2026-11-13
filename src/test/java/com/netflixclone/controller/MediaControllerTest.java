package com.netflixclone.controller;

import com.netflixclone.service.MediaService;
import com.netflixclone.service.StoredMedia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaControllerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void streamsFullVideoWhenRangeIsNotRequested() throws Exception {
        MediaController controller = controllerFor("0123456789");
        MockHttpServletRequest request = new MockHttpServletRequest();

        var response = controller.getVideo("media-id", request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("0123456789", output.toString());
        assertEquals(10, response.getHeaders().getContentLength());
    }

    @Test
    void streamsRequestedByteRange() throws Exception {
        MediaController controller = controllerFor("0123456789");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Range", "bytes=2-5");

        var response = controller.getVideo("media-id", request);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);

        assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
        assertEquals("2345", output.toString());
        assertEquals("bytes 2-5/10", response.getHeaders().getFirst("Content-Range"));
    }

    private MediaController controllerFor(String content) throws Exception {
        Path file = tempDirectory.resolve("media.mp4");
        Files.writeString(file, content);

        MediaService mediaService = mock(MediaService.class);
        when(mediaService.getPublishedVideo("media-id"))
                .thenReturn(new StoredMedia(file, MediaType.parseMediaType("video/mp4")));
        return new MediaController(mediaService);
    }
}
