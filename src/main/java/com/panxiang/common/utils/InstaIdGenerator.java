package com.panxiang.common.utils;

/**
 * @author PanXiang
 * @version 1.0
 * @date 2017-01-05.
 */
public class InstaIdGenerator {
    /**
     * 时间戳的位数，实际占41位，最高位保持为0，保证long值为正数
     */
    private int timestampBitCount = 42;

    /**
     * 逻辑分片位数
     */
    private int regionBitCount = 10;

    /**
     * 逻辑分片的最大数量
     */
    private int regionModelVal = 1 << regionBitCount;

    /**
     * 序列位数
     */
    private int sequenceBitCount = 12;

    /**
     * 总的位数
     */
    private int totalBitCount = timestampBitCount + regionBitCount + sequenceBitCount;

    /**
     * 当前序列值
     */
    private long sequence = 0;

    /**
     * 最后一次请求时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 序列的位板
     */
    private long sequenceMask = -1L ^ (-1L << sequenceBitCount);

    /**
     * 最后一次请求用户标识
     */
    private long lastTag=1L;

    public InstaIdGenerator() {

    }

    public InstaIdGenerator(long seq) {
        if (seq < 0) {
            seq = 0;
        }
        this.sequence = seq;
    }

    public synchronized long nextId(long tag) {
        long timestamp = timeGen();

        if(tag<0){
            tag=-tag;
        }

        if (timestamp < lastTimestamp) {
           // LOG.error(String.format("clock is moving backwards.  Rejecting requests until %d.", lastTimestamp));
            throw new RuntimeException(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        if(tag==lastTag){
            sequence = (sequence + 4) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        lastTag=tag;

        lastTimestamp = timestamp;

        return (timestamp << (totalBitCount - timestampBitCount))
                | ((tag % regionModelVal) << (totalBitCount - timestampBitCount - regionBitCount)) | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        InstaIdGenerator InstaIdGenerator = new InstaIdGenerator(120);
        for (int i = 0; i < 10; i++) {
            long id = InstaIdGenerator.nextId(12);
            System.out.println(id);
        }
    }
}
