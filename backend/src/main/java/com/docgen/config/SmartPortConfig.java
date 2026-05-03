package com.docgen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * 智能端口探测配置。
 *
 * <h3>设计意图</h3>
 * <p>作为一个「开箱即用」的独立工具，用户可能在本地已有其他服务占用了 8080 端口。
 * 传统做法是让用户手动修改配置文件，但这违背了"开箱即用"的产品理念。</p>
 *
 * <h3>探测算法</h3>
 * <p>从默认端口 8080 开始，尝试绑定 ServerSocket。如果绑定失败（端口被占用），
 * 则端口号递增 +1 重试，直到找到可用端口或超过最大尝试次数（100 次）。</p>
 *
 * <h3>为什么用 ServerSocket 而不是其他方式</h3>
 * <p>直接尝试绑定是最可靠的端口可用性检测方式。其他方式（如扫描进程列表）
 * 依赖操作系统特定 API，跨平台兼容性差。ServerSocket 是 JDK 原生 API，
 * 且能真正反映端口的绑定可用性（而非仅检查是否有连接）。</p>
 */
public final class SmartPortConfig {

    private static final Logger log = LoggerFactory.getLogger(SmartPortConfig.class);

    /** 默认起始端口 */
    private static final int DEFAULT_PORT = 8080;

    /** 最大尝试次数，防止无限循环 */
    private static final int MAX_ATTEMPTS = 100;

    private SmartPortConfig() {
        // 工具类禁止实例化
    }

    /**
     * 从默认端口开始探测，返回第一个可用的端口号。
     *
     * @return 可用端口号
     * @throws IllegalStateException 如果连续 {@value MAX_ATTEMPTS} 个端口都不可用
     */
    public static int findAvailablePort() {
        for (int port = DEFAULT_PORT; port < DEFAULT_PORT + MAX_ATTEMPTS; port++) {
            if (isPortAvailable(port)) {
                log.info("端口探测成功: {} 可用", port);
                return port;
            }
            log.debug("端口 {} 已被占用，尝试下一个...", port);
        }

        throw new IllegalStateException(
                String.format("连续 %d 个端口 (%d-%d) 均不可用，请手动指定端口",
                        MAX_ATTEMPTS, DEFAULT_PORT, DEFAULT_PORT + MAX_ATTEMPTS - 1)
        );
    }

    /**
     * 检测指定端口是否可用。
     *
     * <p>通过尝试绑定 {@link ServerSocket} 来判断。绑定成功说明端口空闲，
     * 立即释放后返回 true；绑定失败（抛出 IOException）说明端口被占用。</p>
     *
     * @param port 待检测的端口号
     * @return true 如果端口可用
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            // 设置 SO_REUSEADDR 确保释放后立即可重新绑定
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
