package com.album.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.PutObjectResult;

public class AliyunUtis {

    public static Bucket createBucket(String bucketName) {
        if (org.apache.commons.lang.StringUtils.isBlank(bucketName)) return null;
        OSSClient ossClient = null;
        try {
            ossClient = AliyunUtis.getClient();
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            // 默认权限为私有，改为公共读写
            createBucketRequest.setCannedACL(CannedAccessControlList.PublicReadWrite);
            return ossClient.createBucket(createBucketRequest);
        } finally {
            AliyunUtis.closeClient(ossClient);
        }
    }

    public static void deleteBucket(String bucketName) {
        if (org.apache.commons.lang.StringUtils.isBlank(bucketName)) return;
        OSSClient ossClient = null;
        try {
            ossClient = AliyunUtis.getClient();
            ossClient.deleteBucket(bucketName);
        } finally {
            AliyunUtis.closeClient(ossClient);
        }
    }

    public static boolean doesBucketExist(String bucketName) {
        if (org.apache.commons.lang.StringUtils.isBlank(bucketName)) return false;
        OSSClient ossClient = null;
        try {
            ossClient = AliyunUtis.getClient();
            return ossClient.doesBucketExist(bucketName);
        } finally {
            AliyunUtis.closeClient(ossClient);
        }
    }

    /**
     * @param sourceName 源文件，形如 D:\\1.txt
     * @param objectName 上传到阿里云后的文件名 形如 dir1/dir2/test.txt
     */
    public static PutObjectResult putObject(String sourceName, String objectName) {
        if (org.apache.commons.lang.StringUtils.isBlank(sourceName) ||
            org.apache.commons.lang.StringUtils.isBlank(objectName)) return null;
        OSSClient ossClient = null;
        try {
            ossClient = AliyunUtis.getClient();
            return ossClient.putObject(PropertyUtil.get("aliyun.bucketName"),
                                objectName,
                                new File(sourceName));
        } finally {
            AliyunUtis.closeClient(ossClient);
        }
    }

    /**
     * 递归列出某前缀(相当于目录)下的所有非目录文件的去掉所有前缀信息的简单名称
     * @param prefix 形如 dir1/dir2/ 注意：最开始没有/，最后有/
     * 
     * 例如：
     * 传入前缀为 photos/
     * 则objectSummary.getKey()取出的值形如：
     * 
     * photos/
     * photos/2018-5-1_dd.jpg
     * photos/2018-5-1_ssss.jpg
     * photos/2018-6-1_aa.jpg
     * photos/dd/
     * 
     * 本方法会去掉所有前缀信息，同时过滤掉目录photos/及photos/dd/
     * 最终返回字符串列表：
     * 
     * 2018-5-1_dd.jpg
     * 2018-5-1_ssss.jpg
     * 2018-6-1_aa.jpg
     */
    public static List<String> listObjects(String prefix) {
        if (org.apache.commons.lang.StringUtils.isBlank(prefix)) return null;
        OSSClient ossClient = null;
        List<String> result = null;
        try {
            ossClient = AliyunUtis.getClient();
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(PropertyUtil.get("aliyun.bucketName"));
            listObjectsRequest.setPrefix(prefix);
            // 递归列出prefix下的所有文件
            ObjectListing listing = ossClient.listObjects(listObjectsRequest);
            result = new ArrayList<String>(listing.getObjectSummaries().size());
            // 遍历所有文件
            String separator = "/";
            for (OSSObjectSummary objectSummary : listing.getObjectSummaries()) {
                String name = objectSummary.getKey();
                if (name.endsWith(separator)) continue;
                String[] array = name.split(separator);
                result.add(array[array.length - 1]);
            }
            return result;
        } finally {
            AliyunUtis.closeClient(ossClient);
        }
    }

    @SuppressWarnings("deprecation")
    private static OSSClient getClient() {
        return new OSSClient(PropertyUtil.get("aliyun.endpoint"),
                             PropertyUtil.get("aliyun.accessKeyId"),
                             PropertyUtil.get("aliyun.accessKeySecret"));
    }

    private static void closeClient(OSSClient ossClient) {
        if (null != ossClient) ossClient.shutdown();
    }
}
