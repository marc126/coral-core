package org.coral.core.utils;

/**
 * WGS-84：是国际标准，GPS坐标（Google Earth使用、或者GPS模块）
 * 
 * GCJ-02：中国坐标偏移标准，Google Map、高德、腾讯使用（火星坐标系）
 * 
 * BD-09：百度坐标偏移标准，Baidu Map使用
 * 
 * @author marc
 *
 */
public class GpsUtils {
	final static double pi = 3.14159265358979324;
	final static double x_pi = 52.35987755982988;// 3.14159265358979324 * 3000.0

	/**
	 * WGS-84 to GCJ-02
	 * @param wgsLat
	 * @param wgsLon
	 * @return
	 */
	public static double[] wgs2gcj(double wgsLat, double wgsLon) {
		if (outOfChina(wgsLat, wgsLon))
			return new double[] { wgsLat, wgsLon };
		double[] delta = delta(wgsLat, wgsLon);
		return new double[] { wgsLat + delta[0], wgsLon + delta[1] };
	}

	/**
	 * GCJ-02 to WGS-84(粗略)
	 * @param gcjLat
	 * @param gcjLon
	 * @return
	 */
	public static double[] gcj2wgs(double gcjLat, double gcjLon) {
		if (outOfChina(gcjLat, gcjLon))
			return new double[] { gcjLat, gcjLon };

		double[] d = delta(gcjLat, gcjLon);
		return new double[] { gcjLat - d[0], gcjLon - d[1] };
	}

	/**
	 * GCJ-02 to WGS-84 exactly（精确）
	 * @param gcjLat
	 * @param gcjLon
	 * @return
	 */
	public static double[] gcj2wgsExact(double gcjLat, double gcjLon) {

		double initDelta = 0.01;
		double threshold = 0.000000001;
		double dLat = initDelta, dLon = initDelta;
		double mLat = gcjLat - dLat, mLon = gcjLon - dLon;
		double pLat = gcjLat + dLat, pLon = gcjLon + dLon;
		double wgsLat, wgsLon, i = 0;
		while (true) {
			wgsLat = (mLat + pLat) / 2;
			wgsLon = (mLon + pLon) / 2;
			double[] tmp = gcj2wgs(wgsLat, wgsLon);
			dLat = tmp[0] - gcjLat;
			dLon = tmp[1] - gcjLon;
			if ((Math.abs(dLat) < threshold) && (Math.abs(dLon) < threshold))
				break;

			if (dLat > 0)
				pLat = wgsLat;
			else
				mLat = wgsLat;
			if (dLon > 0)
				pLon = wgsLon;
			else
				mLon = wgsLon;

			if (++i > 10000)
				break;
		}
		return new double[] { wgsLat, wgsLon };
	}

	/**
	 * WGS-84 to DB-09
	 * @param wgsLat
	 * @param wgsLon
	 * @return
	 */
	public static double[] wgs2bd(double wgsLat, double wgsLon){
		double[] m = wgs2gcj(wgsLat, wgsLon);
		return (gcj2db(m[0],m[1]));
	}
	
	/**
	 * DB-09 to WGS-84
	 * @param bdLat
	 * @param bdLon
	 * @return
	 */
	public static double[] db2wgs(double bdLat, double bdLon){
		double[] m = db2gcj(bdLat,bdLon);
		return gcj2wgs(m[0], m[1]);
	}
	
	/**
	 * GCJ-02 to BD-09
	 * @param gcjLat
	 * @param gcjLon
	 * @return
	 */
	public static double[] gcj2db(double gcjLat, double gcjLon) {
		double x = gcjLon, y = gcjLat;
		double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
		double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
		double bdLon = z * Math.cos(theta) + 0.0065;
		double bdLat = z * Math.sin(theta) + 0.006;
		return new double[] { bdLat, bdLon };
	}

	/**
	 * BD-09 to GCJ-02
	 * @param bdLat
	 * @param bdLon
	 * @return
	 */
	public static double[] db2gcj(double bdLat, double bdLon) {
		double x = bdLon - 0.0065, y = bdLat - 0.006;
		double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
		double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
		double gcjLon = z * Math.cos(theta);
		double gcjLat = z * Math.sin(theta);
		return new double[] { bdLat, bdLon };
	}

	/**
	 * 求两点距离
	 * @param latA
	 * @param lonA
	 * @param latB
	 * @param lonB
	 * @return
	 */
	public static double distance(double latA, double lonA, double latB, double lonB) {
		double earthR = 6371000;
		double x = Math.cos(latA * Math.PI / 180) * Math.cos(latB * Math.PI / 180)
				* Math.cos((lonA - lonB) * Math.PI / 180);
		double y = Math.sin(latA * Math.PI / 180) * Math.sin(latB * Math.PI / 180);
		double s = x + y;
		if (s > 1)
			s = 1;
		if (s < -1)
			s = -1;
		double alpha = Math.acos(s);
		double distance = alpha * earthR;
		return distance;
	}

	private static boolean outOfChina(double lat, double lon) {
		if (lon < 72.004 || lon > 137.8347)
			return true;
		if (lat < 0.8293 || lat > 55.8271)
			return true;
		return false;
	}

	private static double transformLat(double x, double y) {
		double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
		ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
		return ret;
	}

	private static double transformLon(double x, double y) {
		double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
		ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
		return ret;
	}

	private static double[] delta(double lat, double lon) {
		// Krasovsky 1940
		//
		// a = 6378245.0, 1/f = 298.3
		// b = a * (1 - f)
		// ee = (a^2 - b^2) / a^2;
		double a = 6378245.0;
		double ee = 0.00669342162296594323;
		double dLat = transformLat(lon - 105.0, lat - 35.0);
		double dLon = transformLon(lon - 105.0, lat - 35.0);
		double radLat = lat / 180.0 * pi;
		double magic = Math.sin(radLat);
		magic = 1 - ee * magic * magic;
		double sqrtMagic = Math.sqrt(magic);
		dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
		dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
		return new double[] { dLat, dLon };
	}
}
