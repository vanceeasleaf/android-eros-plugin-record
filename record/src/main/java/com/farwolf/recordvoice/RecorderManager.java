package com.farwolf.recordvoice;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import omrecorder.AudioChunk;
import omrecorder.AudioSource;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.Recorder;

/**
 * Created by xuqinchao on 17/1/7.
 *  Copyright (c) 2017 Instapp. All rights reserved.
 * 
 */

public class RecorderManager {
    private Recorder mRecorder;
    private File mFile;
    private boolean mIsRecording;
    private boolean mIsPausing;
    public  int AUDIO_SAMPLE_RATE = 8000;


    private int bufferSizeInBytes = 2048;

    private static volatile RecorderManager instance = null;

    private RecorderManager(){
    }

    public static RecorderManager getInstance() {
        if (instance == null) {
            synchronized (RecorderManager.class) {
                if (instance == null) {
                    instance = new RecorderManager();
                }
            }
        }

        return instance;
    }

    public void start(HashMap<String, String> options, ModuleResultListener listener){
        new File(Constant.ROOT_PATH).delete();
        int audioChannel = AudioFormat.CHANNEL_IN_STEREO;
        String channel="mono";
        if(options.containsKey("channel")){
            channel=options.get("channel")+"";
        }
        String quality="";
        if(options.containsKey("quality")){
            quality=options.get("quality")+"";
        }
        if (channel.equals("mono")) audioChannel = AudioFormat.CHANNEL_IN_MONO;
        int sampleRate = 22050;
        int audioBit = AudioFormat.ENCODING_PCM_16BIT;
        switch (quality) {
            case "low":
                sampleRate = 8000;
                AUDIO_SAMPLE_RATE = 8000;
                audioBit = AudioFormat.ENCODING_PCM_8BIT;
                break;
            case "high":
                sampleRate = 44100;
                AUDIO_SAMPLE_RATE = 44100;
                audioBit = AudioFormat.ENCODING_PCM_16BIT;
                break;
        }

        if (mIsRecording) {
            if (mIsPausing) {
                if (mRecorder != null)mRecorder.resumeRecording();
                mIsPausing = false;
                listener.onResult(null);
            } else {
                listener.onResult(Util.getError(Constant.RECORDER_BUSY, Constant.RECORDER_BUSY_CODE));
            }
        } else {
            String time_str = new Date().getTime() + "";
            try {
                mFile = Util.getFile(time_str + ".wav");
            } catch (IOException e) {
                e.printStackTrace();
                listener.onResult(Util.getError(Constant.MEDIA_INTERNAL_ERROR, Constant.MEDIA_INTERNAL_ERROR_CODE));
            }
            mRecorder = OmRecorder.wav(
                    new PullTransport.Default(getMic(audioBit, audioChannel, sampleRate), new PullTransport.OnAudioChunkPulledListener() {
                        @Override
                        public void onAudioChunkPulled(AudioChunk audioChunk) {

                        }
                    }), mFile);
            mRecorder.startRecording();
            mIsRecording = true;
            listener.onResult(null);
        }
    }

    public void pause(ModuleResultListener listener) {
        if (!mIsRecording) {
            listener.onResult(Util.getError(Constant.RECORDER_NOT_STARTED, Constant.RECORDER_NOT_STARTED_CODE));
            return;
        }
        if (mIsPausing) {
            listener.onResult(null);
            return;
        }
        if (mRecorder != null) {
            mRecorder.pauseRecording();
            mIsPausing = true;
            listener.onResult(null);
        }
    }

    public void stop(ModuleResultListener listener) {
        if (!mIsRecording) {
            listener.onResult(Util.getError(Constant.RECORDER_NOT_STARTED, Constant.RECORDER_NOT_STARTED_CODE));
            return;
        }
        if (mRecorder != null) {
            mRecorder.stopRecording();
            mIsPausing = false;
            mIsRecording = false;



            String time_str = new Date().getTime() + "";
            try {
                File f = Util.getFile(time_str + ".wav");
                String npath=f.getAbsolutePath();
                new AudioRecordTask(mFile.getAbsolutePath(),npath,listener).execute();
            } catch (IOException e) {
                e.printStackTrace();
                listener.onResult(Util.getError(Constant.MEDIA_INTERNAL_ERROR, Constant.MEDIA_INTERNAL_ERROR_CODE));
            }

        }
    }

    private AudioSource getMic(int audioBit, int channel, int frequency) {
        return new AudioSource.Smart(MediaRecorder.AudioSource.MIC, audioBit, channel, frequency);
    }

//    class AudioRecordThread implements Runnable {
//
//        String from;
//        String to;
//
//        public AudioRecordThread(String from, String to, ModuleResultListener listener) {
//            this.from = from;
//            this.to = to;
//            this.listener = listener;
//        }
//
//        ModuleResultListener listener;
//
//        public void run() {
//
//
//            copyWaveFile(from, to);//给裸数据加上头文件
//
//
//        }
//    }


    public class AudioRecordTask extends  AsyncTask<String,String,String> {
        String from;
                String to;

        public AudioRecordTask(String from, String to, ModuleResultListener listener) {
            this.from = from;
            this.to = to;
            this.listener = listener;
        }

        ModuleResultListener listener;
        @Override
        protected String doInBackground(String... strings) {
            copyWaveFile(from, to);
            return to;
        }

        @Override
        protected void onPostExecute(String s) {
            HashMap<String, String> result = new HashMap<>();
            String path="";
            try {
                 path = Util.getFilePath(s);
            }catch (IOException e){

            }
            result.put("path",path);
            listener.onResult(result);
        }
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        Log.e("Copy From raw_file", inFilename);
        Log.e("Copy  To wav_file", outFilename);
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = AUDIO_SAMPLE_RATE;
        int channels = 1;
        long byteRate = 16 *  AUDIO_SAMPLE_RATE * channels / 8;
        byte[] data = new byte[bufferSizeInBytes];
        Log.i("inFilename", inFilename);
        Log.i("outFilename", outFilename);
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (1 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
