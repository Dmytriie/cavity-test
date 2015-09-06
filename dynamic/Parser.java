import java.util.*;

class Parser {
    enum Indicator {
        XINITL, XFINAL, XNUM, ZINITL, ZFINAL, ZNUM, ZREF, CENTER, SPAN, POINT, MEASUREMENT;

        static final Map<String, Indicator> indicatorMap = new HashMap<String, Indicator>();
        static {
            indicatorMap.put("-xi", XINITL);
            indicatorMap.put("-xf", XFINAL);
            indicatorMap.put("-xn", XNUM);
            indicatorMap.put("-zi", ZINITL);
            indicatorMap.put("-zf", ZFINAL);
            indicatorMap.put("-zn", ZNUM);
            indicatorMap.put("-r", ZREF);
            indicatorMap.put("-c", CENTER);
            indicatorMap.put("-s", SPAN);
            indicatorMap.put("-n", POINT);
            indicatorMap.put("-t", MEASUREMENT);
        }
    }

    static void Parse(String[] args) throws ArgumentException {
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') { // optional indicator
                if (!Indicator.indicatorMap.containsKey(args[i]))
                    throw new ArgumentException();
                try {
                    switch (Indicator.indicatorMap.get(args[i])) {
                        case XINITL:
                            DynamicTest.xInitl = Double.parseDouble(args[++i]);
                            break;
                        case XFINAL:
                            DynamicTest.xFinal = Double.parseDouble(args[++i]);
                            break;
                        case XNUM:
                            DynamicTest.xNum = Integer.parseInt(args[++i]);
                            break;
                        case ZINITL:
                            DynamicTest.zInitl = Double.parseDouble(args[++i]);
                            break;
                        case ZFINAL:
                            DynamicTest.zFinal = Double.parseDouble(args[++i]);
                            break;
                        case ZNUM:
                            DynamicTest.zNum = Integer.parseInt(args[++i]);
                            break;
                        case ZREF:
                            DynamicTest.zRef = Double.parseDouble(args[++i]);
                            break;
                        case CENTER:
                            VectorNetworkAnalyzer.center = Double.parseDouble(args[++i]);
                            break;
                        case SPAN:
                            VectorNetworkAnalyzer.span = Double.parseDouble(args[++i]);
                            break;
                        case POINT:
                            VectorNetworkAnalyzer.point = Integer.parseInt(args[++i]);
                            break;
                        case MEASUREMENT:
                            VectorNetworkAnalyzer.measurement = args[++i].toUpperCase();
                            if (!Arrays.asList("S11", "S21", "S12", "S22").contains(VectorNetworkAnalyzer.measurement))
                                throw new ArgumentException();
                            break;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new ArgumentException();
                } catch (NumberFormatException e) {
                    throw new ArgumentException();
                }
            } else
                throw new ArgumentException();
        }
    }
}

class ArgumentException extends Exception {
    ArgumentException() {
        super();
    }
}
