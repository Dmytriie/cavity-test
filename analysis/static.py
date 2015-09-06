#!/usr/bin/env python3
# -*- coding:utf-8 -*-

import numpy as np
import matplotlib.pyplot as plt

class S_Parameter(object):
    '''
    A class for plotting s-parameters and fitting resonant curve
    '''

    def __init__(self, path):
        '''
        load settings from slow_control.dat
        '''
        if isinstance(path, str): # transmission
            self.path = path.split('/')
            with open(path + "/slow_control.dat") as fin:
                self.n_repeat = int(fin.readline().split()[-1])
                self.n_sample = int(fin.readline().split()[-1])
                self.center = float(fin.readline().split()[-1]) # MHz
                self.span = float(fin.readline().split()[-1]) # kHz
        else: # reflection
            self.path = [path[0].split('/'), path[1].split('/')]
            self.n_repeat = []
            with open(path[0] + "/slow_control.dat") as fin:
                self.n_repeat.append(int(fin.readline().split()[-1]))
            with open(path[1] + "/slow_control.dat") as fin:
                self.n_repeat.append(int(fin.readline().split()[-1]))

    def transmission_trace(self, fno=0):
        '''
        rebuild data points in a transmission trace
        '''
        if not isinstance(self.path[0], str):
            print("Error: type mismatch! This is for transmission measurement.")
            raise SystemExit
        fname = ('/').join(self.path) + "/{:05d}.dat".format(fno)
        real, imag = np.genfromtxt(fname, unpack=True)
        amp = real**2 + imag**2
        exp = -np.floor(np.log10(amp)).max()
        amp *= np.power(10, exp)
        err = amp * 3e-2 * np.log(10)
        freq = np.linspace(-self.span/2, self.span/2, self.n_sample) # kHz
        return freq, amp, err, int(exp)

    def reflection_circles(self, fno=[0, 0]):
        '''
        rebuild data points in a reflection circle
        '''
        if not isinstance(self.path[0], list):
            print("Error: type mismatch! This is for reflection measurement.")
            raise SystemExit
        fname = ('/').join(self.path[0]) + "/{:05d}.dat".format(fno[0])
        s11 = np.genfromtxt(fname, dtype="f8").flatten().view(dtype="c16")
        fname = ('/').join(self.path[1]) + "/{:05d}.dat".format(fno[1])
        s22 = np.genfromtxt(fname, dtype="f8").flatten().view(dtype="c16")
        return np.vstack((s11, s22))

    def resonant_curve(self, f, f0, df, c):
        '''
        auxiliary function, Cauchy distribution
        '''
        return c / (1 + ((f-f0) * 2 / df)**2)

    def resonance_fit(self, fno=0, quiet=False):
        '''
        fit data points with resonant curve
        '''
        freq, amp, err = self.transmission_trace(fno)[:-1]
        from scipy.optimize import curve_fit
        popt, pcov = curve_fit(self.resonant_curve, freq, amp, p0=[0, 50, amp.max()], sigma=err, absolute_sigma=True)
        fo, fwhm = popt[0], popt[1] # kHz
        dfo1 = np.sqrt(pcov[0,0]) # kHz, statistic error
        dfo2 = 1e-3 * self.center # kHz, systematic error
        dfwhm = np.sqrt(pcov[1,1]) # kHz
        q = (fo + 1e3*self.center) / fwhm
        cov = np.insert(pcov[:-1,:-1], 0, 0, axis=1)
        cov = np.insert(cov, 0, [1e-6*self.center**2, 0, 0], axis=0)
        vec = np.array([1/fwhm, 1/fwhm, -(fo+1e3*self.center)/(fwhm**2)])
        dq = np.sqrt(vec.dot(cov).dot(vec.T))
        res_freq = (fo + 1e3*self.center, dfo1, dfo2) # kHz
        freq_wid = (fwhm, dfwhm) # kHz
        q_value = (q, dq)
        if not quiet:
            print("resonant frequency: {:.3f} +- {:.3f} +- {:.3f} kHz".format(res_freq[0], res_freq[1], res_freq[2]))
            print("full width at half maximum: {:.3f} +- {:.3f} kHz".format(freq_wid[0], freq_wid[1]))
            print("quality factor: {:.3f} +- {:.3f}".format(q_value[0], q_value[1]))
        return popt, res_freq, freq_wid, q_value

    def plot_transmission(self, fno=0, quiet=False, save=False):
        '''
        plot transmission trace, data points and fitting curve
        '''
        plt.close("all")
        fig, ax = plt.subplots()
        freq, amp, err, exp = self.transmission_trace(fno)
        popt = self.resonance_fit(fno, quiet)[0]
        ax.fill_between(freq, amp-err, amp+err, facecolor='b', edgecolor="none")
        ax.plot(freq, self.resonant_curve(freq, popt[0], popt[1], popt[2]), color='w')
        ax.set_xlim([freq.min(), freq.max()])
        ax.set_xlabel(r"$f$ − {:g} MHz [kHz]".format(self.center))
        ax.set_ylabel(r"$|S_{21}|^2$ [$\mathsf{10^{−%d}}$]" % exp)
        if save:
            fname = "static_" + self.path[-3] + '_' + self.path[-1] + "_{:05d}".format(fno)
            self.save_to_file(fname)
        plt.show()

    def plot_reflection(self, fno=[0, 0], save=False):
        '''
        plot reflection circles in polar coordinates
        '''
        plt.close("all")
        fig, ax = plt.subplots(subplot_kw=dict(polar=True))
        reflt = self.reflection_circles(fno)
        ax.plot(np.angle(reflt[0]), np.absolute(reflt[0]), linewidth=2, label=r"$S_{%s}$" % self.path[0][-1][1:])
        ax.plot(np.angle(reflt[1]), np.absolute(reflt[1]), linewidth=2, label=r"$S_{%s}$" % self.path[1][-1][1:])
        ax.set_rmax(1.01)
        ax.legend(loc="center left")
        if save:
            fname = "static_" + self.path[0][-3] + '_' + self.path[0][-1] + "_{:05d}".format(fno[0]) + '_' + self.path[1][-1] + "_{:05d}".format(fno[1])
            self.save_to_file(fname)
        plt.show()

    def save_to_file(self, fname):
        '''
        save plot to disk
        '''
        plt.savefig(fname + ".png")
        print("plot saved to " + fname + ".png")


if __name__ == "__main__":
    path = "../../../experiment/cavities/"
    cav = input("please specify the cavity [(c)ircular, (r)ectangular, (e)lliptic]: ")
    cavity = dict(c="circular", r="rectangular", e="elliptic")
    path += cavity[cav[0]] + "/static/"
    subd = input("please specify the subdirectory(ies) (use comma to separate if two are given): ")

    if ',' not in subd: # transmission
        path += subd
        s_para = S_Parameter(path)
        n = s_para.n_repeat
        fno = 0
        if n > 1:
            fno = int(input("please specify the file number (less than {:d}): ".format(n)))
        s_para.plot_transmission(fno, save=True)
    else: # reflection
        subd = subd.split(',')
        s_para = S_Parameter([path+subd[0].strip(), path+subd[1].strip()])
        n = s_para.n_repeat
        fno = [0, 0]
        if n[0] > 1:
            fno[0] = int(input("please specify the file number for S11 (less than {:d}): ".format(n[0])))
        if n[1] > 1:
            fno[1] = int(input("please specify the file number for S22 (less than {:d}): ".format(n[1])))
        s_para.plot_reflection(fno, save=True)
