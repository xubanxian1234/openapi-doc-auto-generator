package com.docgen;

import com.docgen.config.SmartPortConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.awt.*;
import java.net.URI;

/**
 * OpenAPI 接口文档自动生成工具 — 主启动类。
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li><b>智能端口探测</b>：启动前通过 {@link SmartPortConfig} 探活 Socket，
 *       从 8080 起递增找到可用端口，避免端口冲突导致启动失败。</li>
 *   <li><b>自动唤醒浏览器</b>：监听 {@link ApplicationReadyEvent}，
 *       确保 Spring 上下文完全就绪后再调用 {@link Desktop#browse(URI)}，
 *       而不是在 main 方法中直接打开（那时候服务可能还没启动完成）。</li>
 * </ul>
 *
 * <h3>为什么不用 CommandLineRunner</h3>
 * <p>CommandLineRunner 在 ApplicationContext 刷新完成后执行，但此时内嵌 Tomcat
 * 可能尚未完全就绪。ApplicationReadyEvent 是最安全的时机点。</p>
 */
@SpringBootApplication
public class DocAutoGeneratorApplication {

    private static final Logger log = LoggerFactory.getLogger(DocAutoGeneratorApplication.class);

    private final Environment environment;

    public DocAutoGeneratorApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DocAutoGeneratorApplication.class);

        // 在 Spring 环境初始化前注入可用端口
        int availablePort = SmartPortConfig.findAvailablePort();
        System.setProperty("server.port", String.valueOf(availablePort));

        log.info("===== OpenAPI Doc Generator 正在启动，端口: {} =====", availablePort);
        app.run(args);
    }

    /**
     * 应用完全就绪后自动打开系统默认浏览器。
     *
     * <p>边界处理：
     * <ul>
     *   <li>无头环境（如 Linux Server）不支持 Desktop，此时仅打印地址供用户手动访问</li>
     *   <li>打开浏览器失败不应影响应用正常运行，因此异常被捕获而非抛出</li>
     * </ul>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("server.port", "8080");
        String url = "http://localhost:" + port;

        log.info("===== 应用启动成功！访问地址: {} =====", url);

        openBrowser(url);
    }

    /**
     * 尝试使用 {@link Desktop} API 打开系统默认浏览器。
     * <p>该方法对 HeadlessException 和 UnsupportedOperationException 进行容错，
     * 确保在无 GUI 环境下也能正常运行（只是不会自动打开浏览器）。</p>
     *
     * @param url 要打开的 URL 地址
     */
    private void openBrowser(String url) {
        if (!Desktop.isDesktopSupported()) {
            log.warn("当前环境不支持 Desktop API，请手动打开浏览器访问: {}", url);
            return;
        }

        try {
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                log.warn("当前环境不支持浏览器调起，请手动访问: {}", url);
                return;
            }
            desktop.browse(new URI(url));
            log.info("已自动打开系统默认浏览器");
        } catch (Exception e) {
            log.warn("自动打开浏览器失败，请手动访问: {} (原因: {})", url, e.getMessage());
        }
    }
}
