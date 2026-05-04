package com.docgen.word;

import com.docgen.model.ApiDocumentDTO;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Word 文档生成服务。
 *
 * 作为 Builder 的上层封装，提供从 DTO 到字节流的一步转换，
 * 供 Controller 直接返回给前端下载。
 */
@Service
public class WordGenerationService {

    private static final Logger log = LoggerFactory.getLogger(WordGenerationService.class);

    /**
     * 将解析好的 API 文档 DTO 生成为 Word .docx 字节数组。
     *
     * @param docDTO 解析后的 API 文档数据
     * @return .docx 文件的字节数组
     * @throws IOException 如果文档写入流失败
     */
    public byte[] generate(ApiDocumentDTO docDTO) throws IOException {
        log.info("开始生成 Word 文档: {}", docDTO.getTitle());

        XWPFDocument document = new WordDocumentBuilder()
                .createDocument()
                .addHeaderFooter(docDTO)
                .addCoverPage(docDTO)
                .addAllEndpoints(docDTO)
                .build();

        byte[] result = writeToBytes(document);
        log.info("Word 文档生成完成, 大小: {} bytes", result.length);
        return result;
    }

    /**
     * 将 XWPFDocument 序列化为字节数组。
     * 使用 try-with-resources 确保资源及时释放。
     */
    private byte[] writeToBytes(XWPFDocument document) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.write(out);
            return out.toByteArray();
        } finally {
            document.close();
        }
    }
}
