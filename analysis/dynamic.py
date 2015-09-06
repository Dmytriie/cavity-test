#!/usr/bin/env python3
# -*- coding:utf-8 -*-

import numpy as np
import matplotlib.pyplot as plt

class S21(object):
    '''
    A class for plotting S21 and fitting resonant curve
    '''

    def __init__(self, path):
        '''
        load settings from slow_control.dat
        '''
        self.path = path.split('/')
        with open(path + "/slow_control.dat") as fin:
            self.ini_x = float(fin.readline().split()[-1]) # mm
            self.fin_x = float(fin.readline().split()[-1]) # mm
            self.n_x = int(fin.readline().split()[-1])
            self.ini_z = float(fin.readline().split()[-1]) # mm
            self.fin_z = float(fin.readline().split()[-1]) # mm
            self.n_z = int(fin.readline().split()[-1])
            self.n_sample = int(fin.readline().split()[-1])
            self.center = float(fin.readline().split()[-1]) # MHz
            self.span = float(fin.readline().split()[-1]) # kHz
            self.ref = False if float(fin.readline().split()[-1]) < 0 else True

    def transmission_trace(self, coord=[0, 0], ref=False):
        '''
        rebuild data points in a transmission trace
        '''
        if ref and not self.ref:
            print("Error: no references were measured.")
            raise SystemExit
        fname = ('/').join(self.path) + "/{:d}_{:03d}_{:03d}.dat".format(not ref, coord[0], coord[1])
        real, imag = np.genfromtxt(fname, unpack=True)
        amp = real**2 + imag**2
        exp = -np.floor(np.log10(amp)).max()
        amp *= np.power(10, exp)
        err = amp * 3e-2 * np.log(10)
        freq = np.linspace(-self.span/2, self.span/2, self.n_sample) # kHz
        return freq, amp, err, int(exp)

    def resonant_curve(self, f, f0, df, c):
        '''
        auxiliary function, Cauchy distribution
        '''
        return c / (1 + ((f-f0) * 2 / df)**2)

    def resonance_fit(self, coord=[0, 0], ref=False, quiet=False):
        '''
        fit data points with resonant curve
        '''
        freq, amp, err = self.transmission_trace(coord, ref)[:-1]
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

    def plot_transmission(self, coord=[0,0], ref=False, quiet=False, save=False):
        '''
        plot transmission trace, data points and fitting curve
        '''
        plt.close("all")
        fig, ax = plt.subplots()
        freq, amp, err, exp = self.transmission_trace(coord, ref)
        popt = self.resonance_fit(coord, ref, quiet)[0]
        ax.fill_between(freq, amp-err, amp+err, facecolor='g', edgecolor="none", alpha=1/3)
        ax.plot(freq, self.resonant_curve(freq, popt[0], popt[1], popt[2]), color='b')
        ax.set_xlabel(r"$f$ − {:g} MHz [kHz]".format(self.center))
        ax.set_ylabel(r"$|S_{21}|^2\times 10^{%d}$" % exp)
        if save:
            fname = "dynamic_" + self.path[-3] + '_' + self.path[-1] + "_{:d}_{:03d}_{:03d}".format(not ref, coord[0], coord[1])
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
    cav = input("please specify the cavity [(r)ectangular, (e)lliptic]: ")
    cavity = dict(r="rectangular", e="elliptic")
    path += cavity[cav[0]] + "/dynamic/"
    subd = input("please specify the subdirectory: ")
    path += subd

    s21 = S21(path)
    coord = [0, 0]
    ref = False
    if s21.n_x > 1:
        coord[0] = int(input("please specify the coordinate number of X (less than {:d}): ".format(s21.n_x)))
    if s21.n_z > 1:
        coord[1] = int(input("please specify the coordinate number of Z (less than {:d}): ".format(s21.n_z)))
    if s21.ref:
        ans = input("would you like to inspect the reference measurement (y/n)? ")
        while ans[0] != 'y' and ans[0] != 'n':
            ans = input("unregistered answer, please try again: ")
        ref = True if ans[0] == 'y' else False
    s21.plot_transmission(coord, ref, save=True)
