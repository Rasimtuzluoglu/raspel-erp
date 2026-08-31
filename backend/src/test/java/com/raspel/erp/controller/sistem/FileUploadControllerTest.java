package com.raspel.erp.controller.sistem;

import com.raspel.erp.controller.TestSecurityMocks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileUploadController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityMocks.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadAvatar_bosDosyaReddedilir() throws Exception {
        MockMultipartFile bos = new MockMultipartFile("file", "bos.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/upload/avatar").file(bos))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFoto_gecersizTipReddedilir() throws Exception {
        MockMultipartFile dosya = new MockMultipartFile("file", "dosya.exe", "application/octet-stream", "x".getBytes());
        mockMvc.perform(multipart("/api/upload/foto").file(dosya))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAvatar_olmayanDosya404Doner() throws Exception {
        mockMvc.perform(get("/api/uploads/avatars/bulunmayan.png"))
                .andExpect(status().isNotFound());
    }
}
