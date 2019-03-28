package com.yl.heartratedetectZTShao;


import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import org.opencv.core.Rect;

import org.opencv.core.Size;
import java.util.Vector;

import static com.yl.heartratedetectZTShao.DetectActivity.outFaces;
import static org.opencv.core.Core.BORDER_CONSTANT;
import static org.opencv.core.Core.DFT_ROWS;
import static org.opencv.core.Core.copyMakeBorder;
import static org.opencv.core.Core.idft;
import static org.opencv.core.Core.merge;
import static org.opencv.core.Core.mulSpectrums;
import static org.opencv.core.Core.normalize;
import static org.opencv.core.Core.split;
import static org.opencv.core.CvType.CV_32FC4;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.core.Scalar.all;

import static org.opencv.imgproc.Imgproc.pyrDown;
import static org.opencv.imgproc.Imgproc.pyrUp;
import static org.opencv.imgproc.Imgproc.resize;

/**
 * Created by 邵喆泰 on 2018/4/24.
 */

public class VideoProcessor {
    private int rate;
    private int levels = 4;
    private int alpha = 50;    // 放大倍数
    private double fl = 1.0;    // 下限频率
    private double fh = 1.5;    // 上限频率

    public VideoProcessor(int rate) {
        this.rate = rate;
    }

    void temporalIdealFilter(Mat src, Mat dst) {
        Vector<Mat> channels = new Vector<>();
        split(src, channels);
        for (int i = 0; i < 4; ++i) {

            Mat current = channels.get(i);  // current channel
            Mat tempImg = new Mat();


            int width = Core.getOptimalDFTSize(current.cols());
            int height = Core.getOptimalDFTSize(current.rows());

            copyMakeBorder(current, tempImg, 0, height - current.rows(), 0, width - current.cols(), BORDER_CONSTANT, all(0));
            // do the DFT
            Core.dft(tempImg, tempImg);
            // construct the filter
            Mat filter = tempImg.clone();
            createIdealBandpassFilter(filter, fl, fh, rate);
            // apply filter
            mulSpectrums(tempImg, filter, tempImg, DFT_ROWS);
            // do the inverse DFT on filtered image
            idft(tempImg, tempImg);
            // copy back to the current channel
            Rect rect = new Rect(0, 0, current.cols(), current.rows());
            Mat newTemp = new Mat(tempImg, rect);
            newTemp.copyTo(channels.get(i));
        }
        // merge channels
        merge(channels, dst);

        // normalize the filtered image
        normalize(dst, dst, 0d, 1d, 32);
    }

    /*
     * spatialFilter	-	spatial filtering an image
     *
     * @param src		-	source image
     * @param pyramid	-	destinate pyramid
     */


    Boolean buildGaussianPyramid(Mat img, int levels, Vector<Mat> pyramid) {

        pyramid.clear();
        Mat currentImg = img;
        for (int l = 0; l < levels; l++) {
            Mat down = new Mat();
            pyrDown(currentImg, down);
            pyramid.add(down);
            currentImg = down;
        }
        return true;

    }

    void upsamplingFromGaussianPyramid(Mat src, int levels, Mat dst) {
        Mat currentLevel = src.clone();
        for (int i = 0; i < levels; ++i) {
            Mat up = new Mat();
            pyrUp(currentLevel, up);
            currentLevel = up;
        }
        currentLevel.copyTo(dst);
    }

    /*
     * temporalFilter	-	temporal filtering an image
     *
     * @param src	-	source image
     * @param dst	-	destinate image

       amplify	-	ampilfy the motion

      @param filtered	- motion image
     */

    /*
         * temporalFilter	-	temporal filtering an image
         *
         * @param src	-	source image
         * @param dst	-	destinate image
         amplify	-	ampilfy the motion
         @param filtered	- motion image
         */
    Mat amplify(Mat src) {
        Mat dst = new Mat();
        Scalar a = new Scalar(alpha);
        Core.multiply(src, a, dst);
        return dst;
    }

    /*
     * concat	-	concat all the frames into a single large Mat
     *              where each column is a reshaped single frame
     *
   * @param frames	-	frames of the video sequence
     * @param dst		-	destinate concatnate image
     */
    void concat(Vector<Mat> frames, Mat dst) {
        Size frameSize = frames.get(0).size();
        Mat temp = new Mat((int) (frameSize.height * frameSize.width), outFaces.size() - 1, CV_32FC4);
        for (int i = 0; i < outFaces.size() - 1; ++i) {
            // get a frame if any
            Mat input = frames.get(i);
            // reshape the frame into one column
            // 像素总数不变，但row变成总数，意味着column为1
            Mat reshaped = input.reshape(4, input.cols() * input.rows()).clone();
            //   Mat line = temp.col(i);
            // save the reshaped frame to one column of the destinate big image
            reshaped.copyTo(temp.col(i));
        }
        temp.copyTo(dst);

    }

    /*
     * deConcat	-	de-concat the concatnate image into frames
     *
     * @param src       -   source concatnate image
     * @param framesize	-	frame size
     * @param frames	-	destinate frames
     */
    void deConcat(Mat src, Size frameSize, Vector<Mat> frames) {
        for (int i = 0; i < outFaces.size() - 1; ++i) {    // get a line if any
            Mat line = src.col(i).clone();
            Mat reshaped = line.reshape(4, (int) frameSize.height).clone();
            frames.add(reshaped);
        }
    }

    /*
     * createIdealBandpassFilter	-	create a 1D ideal band-pass filter
     *
     * @param filter    -	destinate filter
     * @param fl        -	low cut-off
     * @param fh		-	high cut-off
     * @param rate      -   sampling rate(i.e. video frame rate)
     */
    void createIdealBandpassFilter(Mat filter, double fl, double fh, double rate) {
        int width = filter.cols();
        int height = filter.rows();

        fl = 2 * fl * width / rate;
        fh = 2 * fh * width / rate;

        double response;

        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                // filter response
                if (j >= fl && j <= fh)
                    response = 1.0f;
                else
                    response = 0.0f;
                filter.put(i, j, response);
            }
        }
    }

    /*
    Moving Average Filtering
     */
    void smooth(Vector<Double> input_data, int len, Vector<Double> output_data, int span) {
        int i = 0, j = 0;
        int pn = 0, n = 0;
        double sum = 0.0;

        if (span % 2 == 1) {
            n = (span - 1) / 2;
        } else {
            n = (span - 2) / 2;
        }

        for (i = 0; i < len; ++i) {
            pn = n;

            if (i < n) {
                pn = i;
            } else if ((len - 1 - i) < n) {
                pn = len - i - 1;
            }

            sum = 0.0;
            for (j = i - pn; j <= i + pn; ++j) {
                sum += input_data.get(j);
            }
            output_data.add(sum / (pn * 2 + 1));
        }
    }

    /**
     * colorMagnify	-	color magnification
     */
    double colorMagnify(Vector<Mat> cheek) {
        // output frame
        Mat output;
        Mat outputTemp;
        // motion image
        Mat motion = new Mat();
        // temp image
        Mat temp = new Mat();

        // video frames
        Vector<Mat> frames = new Vector<>();
        // down-sampled frames
        Vector<Mat> downSampledFrames = new Vector<>();
        // filtered frames
        Vector<Mat> filteredFrames = new Vector<>();
        ;

        // concatenate image of all the down-sample frames
        Mat videoMat = new Mat();
        // concatenate filtered image
        Mat filtered = new Mat();

        // 1. spatial filtering
        for (int i = 0; i < cheek.size(); i++) {

            cheek.get(i).convertTo(temp, CV_32FC4);
            frames.add(temp.clone());

            // spatial filtering
            Vector<Mat> pyramid = new Vector<>();
            buildGaussianPyramid(temp, levels, pyramid);
            downSampledFrames.add(pyramid.get(levels - 1));   //存入缩小图片
        }

        // 2. concat all the frames into a single large Mat
        // where each column is a reshaped single frame
        // (for processing convenience)
        concat(downSampledFrames, videoMat);

        // 3. temporal filtering
        temporalIdealFilter(videoMat, filtered);
        // 4. amplify color motion
        Mat amplifiedFiltered =amplify(filtered);

        // 5. de-concat the filtered image into filtered frames
        deConcat(amplifiedFiltered, downSampledFrames.get(0).size(), filteredFrames);

        Vector<Mat> cl = new Vector<>();

        Vector<Double> markdataG = new Vector<>();


        // 6. amplify each frame
        // by adding frame image and motions
        // and write into video

        for (int i = 0; i < cheek.size() - 1; ++i) {
            // up-sample the motion image
            upsamplingFromGaussianPyramid(filteredFrames.get(i), levels, motion);
            resize(motion, motion, frames.get(i).size());
            motion.convertTo(motion, CV_32FC4);
            Core.add(frames.get(i), motion, temp);
            output = temp.clone();
            output.convertTo(output, CV_8UC4);

            split(output, cl);

            Scalar meanGreen = Core.mean(cl.get(0));
            markdataG.add(meanGreen.val[0]);

        }
        Vector<Double> cutMarkDataB = new Vector<>();
        for (int i = 0; i < markdataG.size(); i++) {
            cutMarkDataB.add(markdataG.get(i));
        }

        Complex A=new Complex(rate);
        Complex[] pulseCalculate = new Complex[cutMarkDataB.size()];
        for (int i = 0; i < cutMarkDataB.size(); i++) {
            pulseCalculate[i] = new Complex(cutMarkDataB.get(i), 0);
        }
        Complex []result=A.dft(pulseCalculate);
        double [] abs=A.abs(result);
        double pulse=A.pulse(abs);
        return pulse;

    }
}






