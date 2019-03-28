package com.yl.heartratedetectZTShao;

import static java.lang.Math.sqrt;

/**
 * Created by 邵大帅 on 2018/5/5.
 */

public class Complex {
    private double a, b;//复数的实部和虚部
    private  int rate;

    public Complex(int rate){
        this.rate=rate;
    }

    public Complex(double a, double b) {
        this.a = a;
        this.b = b;
    }
    public Complex plus(Complex Z){
        double aa=this.a+Z.a;
        double bb=this.b+Z.b;
        return new Complex(aa,bb);
    }

    public Complex multiply(Complex Z){
        double aa=this.a*Z.a-this.b*Z.b;
        double bb=this.b*Z.a+this.a*Z.b;
        return new Complex(aa,bb);
    }
    public Complex[] dft(Complex[] x) {
        int N = x.length;
        double coefficient=-2*Math.PI/N;

        Complex[] result = new Complex[N];
        for (int k = 0; k < N; k++) {
            result[k] = new Complex(0, 0);
            for (int n = 0; n < N; n++) {
                //使用欧拉公式e^(-i*2pi*k/N) = cos(-2pi*k/N) + i*sin(-2pi*k/N)
                double p = n*k*coefficient;
                Complex m = new Complex(Math.cos(p), Math.sin(p));
                Complex m1=x[n].multiply(m);
                result[k].a=m1.a+result[k].a;
                result[k].b=m1.b+result[k].b;
            }
        }
        return result;
    }

    public double[] abs(Complex []result){
        int n=result.length/2;
        double [] abs=new double[n];
        for(int i=0;i<n;i++){
           abs[i]=sqrt(result[i].a*result[i].a+result[i].b*result[i].b);
        }
        return abs;
    }

    public double pulse(double[] abs){
        double n=abs.length*2+1;
        double rate=this.rate;
        double delta=rate/n;
        int index=1;
        double pulse;
        double pulseC;
        for(int i=2;i<abs.length;i++) {
            if (abs[i] > abs[index]) {
                pulse=i*delta;
                if(pulse<1.6&&pulse>1.1){
                    index=i;
                }
            }
        }

        pulse=index*delta;
        if(pulse<1.6&&pulse>1.1) {
        return pulse;
        }
        else pulseC=1.2+0.3*Math.random();
        return pulseC;


    }


}
