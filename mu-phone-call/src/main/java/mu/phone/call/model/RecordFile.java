package mu.phone.call.model;

/**
 * 录音文件实体
 * @author LiYejun
 * @date 2023/6/6
 */
public class RecordFile implements Comparable<RecordFile> {

    private String name;

    private long size;

    private String path;

    private long lastModified;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public int compareTo(RecordFile o) {
        long value = o.getLastModified() / 1000 - getLastModified() / 1000;
        return (int) value;
    }
}
