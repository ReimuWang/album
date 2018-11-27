package com.album.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import net.coobird.thumbnailator.Thumbnails;

import com.album.model.JsonDataHandle;
import com.album.utils.AliyunUtis;
import com.album.utils.ImgUtils;
import com.album.utils.PropertyUtil;

/**
 * 本类负责编写album上传全部的流程
 * 下文中出现的，形如local.sourceDir，local.format这样的值，均为配置文件config.properties中的key
 * 
 * 每次上传时，需要配合本类进行的操作如下
 * 
 * 程序第1次执行(若手动格式化名称，则无需本次执行；执行前需设置local.type)
 * 上传前，若本次上传涉及到了对阿里云中已有图片的重命名，则需删除阿里云中已有的图片(相当于重新上传一次。为保证本地硬盘中的原始文件与阿里云上压缩文件的一致性，始终都要从第一步开始执行，严禁跳步)
 * 1.将本次待上传的图片存入local.sourceDir，直接存入该目录下，不要产生多余的层级
 * 
 * 2.格式化图片名，格式规则为：
 * yyyymmdd_hhmmss_主题_描述
 * 因为采用_作为分隔符，因此非分隔符部分禁止出现_
 * yyyymmdd必须填入一个准确值，例如20181014
 * hhmmss中若某部分没有，则填入99，代表无值。例如071899表示7点18分，秒数未知。按照常理，规则要求，若填入秒，则必须填入时分；若填入分，则必须填入时
 * 图片最终将以yyyymmdd_hhmmss所代表的时刻进行排序，因此若想调整图片展示顺序，则可灵活调整该值(因为yyyymmdd是精确要求的，因此通常就是改变hhmmss)。若时刻无法排序，则基于对应操作系统的取图顺序
 * 主题代表一个事件，例如 日本旅游。相当于一个简易的小图集。若该图片没有所属事件，则填入0
 * 描述是该图片的叙述信息，若无值，则填入0
 * 完整示例：
 * 图片格式：20181014_214599_表白_今天我像女朋友表白啦，而且成功啦~开心
 * 若原始图片较少，或图片名称无规律，则需手动进行图片格式化，无需本次执行
 * 否则，则依原始图片名称规则的不同分别设计格式化方法，完成图片名称的格式化。此时需设定格式化规则local.format，本次上传图片的主题(formatImgName参数)，描述默认为0
 * 
 * 程序第2次执行(执行该步前需保证local.sourceDir中图片的名称已完成格式化，并设置local.type)
 * 3.将local.sourceDir中的图片处理后复制入：
 * local.photoDir：大图，需处理为正方形(补充黑色)。为了节约空间及流量，在尽量不失真的前提下进行压缩
 * local.minPhotoDir：缩略图，需处理为正方形(补充黑色)。
 * 复制后图片名称不变
 * 执行后，确认一下对图片质量是否满意，若不满意，则调整local.photoQuality后重新进行
 * 
 * 程序第3次执行(本次执行前需保证local.photoDir,local.minPhotoDir中已有待上传的图片，且这些图片当前均未在阿里云中)
 * 若需上传的数据为视频，则无需执行前3步，直接从本步开始执行。此时需手动为视频准备一张缩略图并存入local.minPhotoDir
 * 4.完成图片上传：
 * local.minPhotoDir --> aliyun.minPhotoDir
 * local.photoDir --> aliyun.photoDir
 * 
 * 5.生成博客所需json文件
 */
public class Main {

    private static void formatImgName(String subject) {
        File sourceDir = new File(PropertyUtil.get("local.sourceDir"));
        for(File img : sourceDir.listFiles()) {
            String dest = null;
            switch (Integer.parseInt(PropertyUtil.get("local.format"))) {
            case 0:
                dest = Main.formatXiaomiPhoto(img.getName(), subject);
                break;
            }
            img.renameTo(new File(PropertyUtil.get("local.sourceDir") + dest));
        }
    }

    /**
     * @param name 小米手机拍照，例如，IMG_20180811_080810 -- IMG_yyyymmdd_hhmmss
     * @param subject
     */
    private static String formatXiaomiPhoto(String name, String subject) {
        StringBuilder result = new StringBuilder();
        String[] array = name.split("\\.");
        result.append(array[0].substring(4))
              .append("_")
              .append(subject)
              .append("_0.")
              .append(array[1]);
        return result.toString();
    }

    private static void copyAndFormatImg() throws IOException {
        File sourceDir = new File(PropertyUtil.get("local.sourceDir"));
        for(File sourceFile : sourceDir.listFiles()) {
            BufferedImage sourceImg = ImageIO.read(sourceFile);
            BufferedImage squareImg = ImgUtils.toSquare(sourceImg);
            // photo
            float photoQuality = Float.parseFloat(PropertyUtil.get("local.photoQuality"));
            Thumbnails.of(squareImg).scale(1f)
                                    .outputQuality(photoQuality)
                                    .toFile(PropertyUtil.get("local.photoDir") + sourceFile.getName());
            // minPhoto
            int minWidth = Integer.parseInt(PropertyUtil.get("local.minPhotoWidth"));
            Thumbnails.of(squareImg).size(minWidth, minWidth)
                                    .toFile(PropertyUtil.get("local.minPhotoDir") + sourceFile.getName());
            System.out.println(sourceFile.getName());
        }
    }

    private static void upload() {
        Main.upload(PropertyUtil.get("local.photoDir"), PropertyUtil.get("aliyun.photoDir"));
        Main.upload(PropertyUtil.get("local.minPhotoDir"), PropertyUtil.get("aliyun.minPhotoDir"));
    }

    /**
     * 将localDirPath中的文件全部上传至aliyunDirPath
     */
    private static void upload(String localDirPath, String aliyunDirPath) {
        File localDir = new File(localDirPath);
        for(File img : localDir.listFiles()) {
            AliyunUtis.putObject(img.getAbsolutePath(), aliyunDirPath + img.getName());
            System.out.println(img.getAbsolutePath());
        }
    }

    private static void createJson() throws IOException {
        List<String> nameList = AliyunUtis.listObjects(PropertyUtil.get("aliyun.minPhotoDir"));
        JsonDataHandle jsonDataHandle = new JsonDataHandle();
        for (String name : nameList) jsonDataHandle.add(name);
        jsonDataHandle.createJson();
        
    }

    public static void main(String[] args) throws IOException {
        switch (Integer.parseInt(PropertyUtil.get("local.type"))) {
        case 0:
            Main.formatImgName("IDO漫展");
            break;
        case 1:
            Main.copyAndFormatImg();
            break;
        case 2:
            Main.upload();
            Main.createJson();
            break;
        }
    }
}
