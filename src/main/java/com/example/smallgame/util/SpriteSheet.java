package com.example.smallgame.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * 立绘帧工具：按资源目录加载 GIF 拆帧后的 PNG 序列，
 * 提供帧索引获取与 Canvas 绘制（支持水平镜像、旋转与缩放）。
 * 加载结果按目录缓存，避免重复读取资源。
 * <p>
 * 被 MonsterAppearance（怪物立绘）、WeaponDrop（武器立绘）、
 * CharacterSelectView / GameMainView（角色立绘）等视图与实体引用。
 */
public final class SpriteSheet {
    /** 每个角色立绘的帧数（官方 GIF 均为 8 帧）。 */
    public static final int FRAME_COUNT = 8;

    /** 按资源目录缓存已加载的帧序列 */
    private static final Map<String, SpriteSheet> CACHE = new HashMap<>();

    /** 帧图像列表 */
    private final List<Image> frames;

    /**
     * 私有构造，仅由 loadFrames 创建实例。
     *
     * @param frames 帧图像列表
     */
    private SpriteSheet(List<Image> frames) {
        this.frames = frames;
    }

    /**
     * 从 classpath 资源目录（如 /sprites/knight）加载帧序列，带缓存。
     * 通过 computeIfAbsent 保证同一目录只加载一次。
     *
     * @param resourceFolder 资源目录路径
     * @return 帧序列实例
     */
    public static SpriteSheet load(String resourceFolder) {
        return CACHE.computeIfAbsent(resourceFolder, SpriteSheet::loadFrames);
    }

    /**
     * 实际加载帧序列：从 frame0.png 起顺序读取，直到资源不存在为止；
     * 若一帧都未读到则抛出异常，提示资源缺失。
     *
     * @param resourceFolder 资源目录路径
     * @return 帧序列实例
     */
    private static SpriteSheet loadFrames(String resourceFolder) {
        List<Image> loaded = new ArrayList<>(FRAME_COUNT);
        for (int i = 0; ; i++) {
            String path = resourceFolder + "/frame" + i + ".png";
            InputStream stream = SpriteSheet.class.getResourceAsStream(path);
            if (stream == null) {
                if (loaded.isEmpty()) {
                    throw new IllegalStateException("缺少立绘帧资源: "
                            + resourceFolder + "/frame0.png");
                }
                break;
            }
            loaded.add(new Image(stream));
        }
        return new SpriteSheet(loaded);
    }

    /** 帧总数。 */
    public int frameCount() {
        return frames.size();
    }

    /**
     * 取指定索引的帧（自动环绕）。
     * 使用 floorMod 处理负索引与越界索引，保证动画循环播放。
     *
     * @param index 帧索引
     * @return 对应帧图像
     */
    public Image frame(int index) {
        return frames.get(Math.floorMod(index, frames.size()));
    }

    /**
     * 在 Canvas 上绘制立绘帧。
     * 先平移画布到脚底坐标，水平镜像通过 scale(-1, 1) 实现，
     * 图像以中心为轴、脚底为基准绘制。
     *
     * @param g           画布上下文
     * @param frameIndex  帧索引
     * @param centerX     立绘中心 X（世界坐标）
     * @param bottomY     立绘脚底 Y（世界坐标）
     * @param displaySize 立绘显示尺寸（正方形边长）
     * @param flip        是否水平镜像
     */
    public void draw(GraphicsContext g, int frameIndex, double centerX, double bottomY,
                     double displaySize, boolean flip) {
        Image image = frame(frameIndex);
        g.save();
        g.translate(centerX, bottomY);
        if (flip) {
            g.scale(-1, 1);
        }
        g.drawImage(image, -displaySize / 2, -displaySize, displaySize, displaySize);
        g.restore();
    }

    /**
     * 以锚点为中心、按指定角度旋转绘制单张立绘（武器等）。
     * 变换顺序：平移至锚点 -> 按朝向角旋转 -> 可选垂直镜像 -> 叠加立绘角度矫正 -> 绘制。
     * 垂直镜像与角度矫正均在旋转后的坐标系中叠加，保证朝左时立绘仍保持正向。
     *
     * @param g                   画布上下文
     * @param image               立绘图像
     * @param anchorX             锚点 X（世界坐标）
     * @param anchorY             锚点 Y（世界坐标）
     * @param displayWidth        显示宽度
     * @param displayHeight       显示高度
     * @param forwardOffset       沿朝向正方向的前移量（锚点不在图像中心时使用）
     * @param angleRadians        朝向角度（弧度，0 表示朝右）
     * @param spriteOffsetDegrees 立绘自身角度矫正（度，正值顺时针，弓立绘为 -45）
     * @param flipVertical        是否垂直镜像（朝左时保持立绘正向）
     */
    public static void drawRotated(GraphicsContext g, Image image,
            double anchorX, double anchorY, double displayWidth, double displayHeight,
            double forwardOffset, double angleRadians, double spriteOffsetDegrees,
            boolean flipVertical) {
        g.save();
        g.translate(anchorX, anchorY);
        g.rotate(Math.toDegrees(angleRadians));
        if (flipVertical) {
            g.scale(1, -1);
        }
        g.rotate(spriteOffsetDegrees);
        g.drawImage(image, -displayWidth / 2 + forwardOffset, -displayHeight / 2,
                displayWidth, displayHeight);
        g.restore();
    }
}
