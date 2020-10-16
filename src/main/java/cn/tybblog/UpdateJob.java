package cn.tybblog;

import cn.tybblog.util.OkHttpUtil;
;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UpdateJob implements Job {
    private static Logger log= LoggerFactory.getLogger(UpdateJob.class);
    String url = "https://terraria.org/";

    public void execute(JobExecutionContext context) {
        OkHttpUtil.getOkHttp(url, new Callback() {

            public void onFailure(Call call, IOException e) {
                log.error("首页请求失败，错误:{}",e.getMessage());
            }

            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    log.error("请求首页，未知错误");
                    return;
                }
                Document doc = Jsoup.parse(response.body().string());
                Elements elements = doc.select(".page-footer a");
                Element element = elements.get(1);
                url += element.attr("href");
                log.info("获取下载地址成功：{}",url);
                getZip();
            }
        });
    }

    private void getZip(){
        OkHttpUtil.getOkHttp(url, new Callback() {
            public void onFailure(Call call, IOException e) {
                log.error("下载错误：{}",e.getMessage());
            }

            public void onResponse(Call call, Response response) throws IOException {
                String path = UpdateJob.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                path = path.substring(0,path.lastIndexOf('/')) + url.substring(url.lastIndexOf('/'), url.lastIndexOf('?'));
                File file = new File(path);
                if (file.exists()) {
                    log.info("已经是最新版了不需要更新：{}",path);
                    return;
                }
                if (response.isSuccessful()) {
                    log.info("开始下载，文件路径：{}",path);
                    File zip = writeFile(response, path);
                    log.info("下载成功，文件路径：{}",path);
                    Runtime.getRuntime().exec("cmd.exe /C start wmic process where name='conhost.exe' call terminate");
                    String windows = unZip(zip) + "/Windows/";
                    File config = new File(windows+"serverconfig.txt");
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config),"utf-8"));
                    bw.write("priority=1\n" +
                            //世界文件存放路径
                            "world=C:/Users/Administrator/Documents/My Games/Terraria/Worlds/世界名称.wld\n" +
                            //自动创建世界大小
                            "autocreate=2\n" +
                            "worldname=世界名称\n" +
                            //难度
                            "difficulty=0\n" +
                            //最大玩家数
                            "maxplayers=5\n" +
                            //密码
                            "password=xiezi\n" +
                            //世界文件存放文件夹
                            "worldpath=C:/Users/Administrator/Documents/My Games/Terraria/Worlds");
                    bw.close();
                    Runtime.getRuntime().exec("cmd /c start start-server.bat", null, new File(windows));
                }
            }
        });
    }

    private File writeFile(Response response, String path) {
        InputStream is = null;
        FileOutputStream fos = null;
        is = response.body().byteStream();
        File file = new File(path);
        try {
            fos = new FileOutputStream(file);
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = is.read(bytes)) != -1) {
                fos.write(bytes,0,len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
    /**
     * zip解压
     * @param srcFile        zip源文件
     * @throws RuntimeException 解压失败会抛出运行时异常
     */

    public static String unZip(File srcFile) throws RuntimeException {
        String fristDir=null;
        long start = System.currentTimeMillis();
        if (!srcFile.exists()) {
            log.error("{}文件不存在",srcFile.getPath());
        }
        String destDirPath=srcFile.getPath().substring(0,srcFile.getPath().lastIndexOf('\\'));
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(srcFile);
            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    String dirPath = destDirPath + "/" + entry.getName();
                    File dir = new File(dirPath);
                    dir.mkdirs();
                    if (fristDir == null) {
                        fristDir=dir.getPath();
                    }
                } else {
                    File targetFile = new File(destDirPath + "/" + entry.getName());
                    if(!targetFile.getParentFile().exists()){
                        targetFile.getParentFile().mkdirs();
                    }
                    targetFile.createNewFile();
                    InputStream is = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    byte[] buf = new byte[1024];
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    fos.close();
                    is.close();
                }
            }
            long end = System.currentTimeMillis();
            log.info("解压完成，耗时：{} ms",end - start);
        } catch (Exception e) {
            log.error("解压失败：{}",e.getMessage());
        } finally {
            if(zipFile != null){
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fristDir;
    }
}