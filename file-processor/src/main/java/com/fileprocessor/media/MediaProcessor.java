package com.fileprocessor.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Media processing utility using FFmpeg
 */
public class MediaProcessor {

    private static final Logger log = LoggerFactory.getLogger(MediaProcessor.class);

    /**
     * Get video information using ffprobe
     */
    public static VideoInfo getVideoInfo(String filePath) {
        log.info("Getting video info: {}", filePath);

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("File not found: {}", filePath);
                return null;
            }

            VideoInfo info = new VideoInfo();
            info.setFormat(getExtension(filePath));
            info.setFileSize(file.length());

            // Use ffprobe to get info
            String cmd = String.format("ffprobe -v error -show_entries format=duration:stream=codec_name,width,height,r_frame_rate,bit_rate -of default=noprint_wrappers=1 %s", filePath);
            String output = executeCommand(cmd);

            if (output != null) {
                // Parse duration
                Pattern durationPattern = Pattern.compile("duration=(\\d+\\.?\\d*)");
                Matcher m = durationPattern.matcher(output);
                if (m.find()) {
                    info.setDuration(Double.parseDouble(m.group(1)));
                }

                // Parse width/height
                Pattern widthPattern = Pattern.compile("width=(\\d+)");
                m = widthPattern.matcher(output);
                if (m.find()) {
                    info.setWidth(Integer.parseInt(m.group(1)));
                }

                Pattern heightPattern = Pattern.compile("height=(\\d+)");
                m = heightPattern.matcher(output);
                if (m.find()) {
                    info.setHeight(Integer.parseInt(m.group(1)));
                }

                // Parse video codec
                Pattern videoCodecPattern = Pattern.compile("STREAM.*?\\[0\\].*?codec_name=(\\w+)");
                m = videoCodecPattern.matcher(output);
                if (m.find()) {
                    info.setVideoCodec(m.group(1));
                }

                // Parse bitrate
                Pattern bitratePattern = Pattern.compile("bit_rate=(\\d+)");
                m = bitratePattern.matcher(output);
                if (m.find()) {
                    info.setVideoBitrate(Long.parseLong(m.group(1)));
                }
            }

            log.info("Video info extracted: {}x{} @ {}s", info.getWidth(), info.getHeight(), info.getDuration());
            return info;

        } catch (Exception e) {
            log.error("Failed to get video info: {}", filePath, e);
            return null;
        }
    }

    /**
     * Get audio information
     */
    public static AudioInfo getAudioInfo(String filePath) {
        log.info("Getting audio info: {}", filePath);

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("File not found: {}", filePath);
                return null;
            }

            AudioInfo info = new AudioInfo();
            info.setFormat(getExtension(filePath));
            info.setFileSize(file.length());

            // Use ffprobe to get info
            String cmd = String.format("ffprobe -v error -show_entries format=duration:stream=codec_name,sample_rate,channels,bit_rate -of default=noprint_wrappers=1 %s", filePath);
            String output = executeCommand(cmd);

            if (output != null) {
                // Parse duration
                Pattern durationPattern = Pattern.compile("duration=(\\d+\\.?\\d*)");
                Matcher m = durationPattern.matcher(output);
                if (m.find()) {
                    info.setDuration(Double.parseDouble(m.group(1)));
                }

                // Parse codec
                Pattern codecPattern = Pattern.compile("codec_name=(\\w+)");
                m = codecPattern.matcher(output);
                if (m.find()) {
                    info.setCodec(m.group(1));
                }

                // Parse sample rate
                Pattern sampleRatePattern = Pattern.compile("sample_rate=(\\d+)");
                m = sampleRatePattern.matcher(output);
                if (m.find()) {
                    info.setSampleRate(Integer.parseInt(m.group(1)));
                }

                // Parse channels
                Pattern channelsPattern = Pattern.compile("channels=(\\d+)");
                m = channelsPattern.matcher(output);
                if (m.find()) {
                    info.setChannels(Integer.parseInt(m.group(1)));
                }

                // Parse bitrate
                Pattern bitratePattern = Pattern.compile("bit_rate=(\\d+)");
                m = bitratePattern.matcher(output);
                if (m.find()) {
                    info.setBitrate(Long.parseLong(m.group(1)));
                }
            }

            log.info("Audio info extracted: {}Hz, {}ch, {}s", info.getSampleRate(), info.getChannels(), info.getDuration());
            return info;

        } catch (Exception e) {
            log.error("Failed to get audio info: {}", filePath, e);
            return null;
        }
    }

    /**
     * Extract thumbnail from video
     */
    public static boolean extractThumbnail(String sourcePath, String targetPath,
                                           double timestamp, Integer width, Integer height) {
        log.info("Extracting thumbnail from {} at {}s", sourcePath, timestamp);

        try {
            File target = new File(targetPath);
            target.getParentFile().mkdirs();

            String scale = "";
            if (width != null && height != null) {
                scale = String.format("-vf scale=%d:%d", width, height);
            }

            String cmd = String.format("ffmpeg -i %s -ss %f -vframes 1 %s %s",
                    sourcePath, timestamp, scale, targetPath);

            String output = executeCommand(cmd);
            log.info("Thumbnail extracted to: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to extract thumbnail", e);
            return false;
        }
    }

    /**
     * Transcode video using FFmpeg
     */
    public static boolean transcodeVideo(String sourcePath, String targetPath,
                                         TranscodeConfig config) {
        log.info("Transcoding video: {} -> {}", sourcePath, targetPath);

        try {
            File target = new File(targetPath);
            target.getParentFile().mkdirs();

            StringBuilder cmd = new StringBuilder();
            cmd.append("ffmpeg -i ").append(sourcePath);

            if (config.getVideoCodec() != null) {
                cmd.append(" -c:v ").append(config.getVideoCodec());
            }
            if (config.getAudioCodec() != null) {
                cmd.append(" -c:a ").append(config.getAudioCodec());
            }
            if (config.getWidth() != null && config.getHeight() != null) {
                cmd.append(" -s ").append(config.getWidth()).append("x").append(config.getHeight());
            }
            if (config.getVideoBitrate() != null) {
                cmd.append(" -b:v ").append(config.getVideoBitrate());
            }
            if (config.getAudioBitrate() != null) {
                cmd.append(" -b:a ").append(config.getAudioBitrate());
            }

            cmd.append(" ").append(targetPath);

            executeCommand(cmd.toString());
            log.info("Video transcoded to: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to transcode video", e);
            return false;
        }
    }

    /**
     * Transcode audio using FFmpeg
     */
    public static boolean transcodeAudio(String sourcePath, String targetPath,
                                         AudioTranscodeConfig config) {
        log.info("Transcoding audio: {} -> {}", sourcePath, targetPath);

        try {
            File target = new File(targetPath);
            target.getParentFile().mkdirs();

            StringBuilder cmd = new StringBuilder();
            cmd.append("ffmpeg -i ").append(sourcePath);

            if (config.getCodec() != null) {
                cmd.append(" -c:a ").append(config.getCodec());
            }
            if (config.getBitrate() != null) {
                cmd.append(" -b:a ").append(config.getBitrate());
            }
            if (config.getSampleRate() != null) {
                cmd.append(" -ar ").append(config.getSampleRate());
            }
            if (config.getChannels() != null) {
                cmd.append(" -ac ").append(config.getChannels());
            }

            cmd.append(" ").append(targetPath);

            executeCommand(cmd.toString());
            log.info("Audio transcoded to: {}", targetPath);
            return true;

        } catch (Exception e) {
            log.error("Failed to transcode audio", e);
            return false;
        }
    }

    /**
     * Check if file is a video
     */
    public static boolean isVideo(String filePath) {
        String ext = getExtension(filePath).toLowerCase();
        return ext.matches("mp4|avi|mov|mkv|flv|wmv|webm|m4v|mpeg|mpg|3gp");
    }

    /**
     * Check if file is an audio
     */
    public static boolean isAudio(String filePath) {
        String ext = getExtension(filePath).toLowerCase();
        return ext.matches("mp3|wav|aac|flac|ogg|m4a|wma|aiff|opus");
    }

    private static String executeCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(command);

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder error = new StringBuilder();
        while ((line = errorReader.readLine()) != null) {
            error.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("Command exited with code {}: {}", exitCode, error.toString());
        }

        return output.toString();
    }

    private static String getExtension(String filePath) {
        if (filePath == null) return "";
        int lastDot = filePath.lastIndexOf('.');
        return lastDot > 0 ? filePath.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * Transcode configuration for video
     */
    public static class TranscodeConfig {
        private String format = "mp4";
        private String videoCodec = "h264";
        private String audioCodec = "aac";
        private Integer width;
        private Integer height;
        private Long videoBitrate;
        private Long audioBitrate;
        private Double frameRate;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getVideoCodec() { return videoCodec; }
        public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }
        public String getAudioCodec() { return audioCodec; }
        public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        public Long getVideoBitrate() { return videoBitrate; }
        public void setVideoBitrate(Long videoBitrate) { this.videoBitrate = videoBitrate; }
        public Long getAudioBitrate() { return audioBitrate; }
        public void setAudioBitrate(Long audioBitrate) { this.audioBitrate = audioBitrate; }
        public Double getFrameRate() { return frameRate; }
        public void setFrameRate(Double frameRate) { this.frameRate = frameRate; }
    }

    /**
     * Transcode configuration for audio
     */
    public static class AudioTranscodeConfig {
        private String format = "mp3";
        private String codec;
        private Long bitrate;
        private Integer sampleRate;
        private Integer channels;

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getCodec() { return codec; }
        public void setCodec(String codec) { this.codec = codec; }
        public Long getBitrate() { return bitrate; }
        public void setBitrate(Long bitrate) { this.bitrate = bitrate; }
        public Integer getSampleRate() { return sampleRate; }
        public void setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; }
        public Integer getChannels() { return channels; }
        public void setChannels(Integer channels) { this.channels = channels; }
    }
}
