package com.contract.harvest;

import com.contract.harvest.tools.Arith;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class jisuan {

    private static Integer position_num = 0;
    private static float basis_close_percent = (float) 0.0065;
    private static float basis_percent_after = (float) 0.004;

    public static void main(String[] args) {
//          Map<String,String> param =  new TreeMap<>();
//          param.put("user_name","321327476@qq.com");
//          param.put("appid","sinaoem");
//          String secret = "d#$%^&GH^&*dd321";

//        int i=6;int j=4;float k=2;
//        System.out.println(i / j);
//        System.exit(0);
//        get_per((float) 0,(float) 0.065);//1
//        get_per((float) 0.0066,(float) 0.0066);//1
//        get_per((float) 0.0066,(float) 0.011);//1
//        get_per((float) 0.011,(float) 0.0066);//1
//
//        double a = 2;
//        double b = 1;

//        季度 ："ask":[190.543,30],"bid":[190.498,148]
//        次周 ："ask":[190.483,111],"bid":[190.158,100]
//        季度空 次周多
//        季度
//        System.out.println(Arith.getStrBigDecimal(Arith.arithProfit(10,190.498,190.543,10,"short")));
//        System.out.println(Arith.getStrBigDecimal(Arith.arithProfit(10,190.483,190.158,10,"long")));
//
//        System.out.println(Arith.getStrBigDecimal(Arith.arithBasisPercent(190.543,190.158)));
//        System.out.println(Arith.getStrBigDecimal(Arith.arithBasisPercent(190.498,190.483)));
//
//        System.out.println(Arith.getStrBigDecimal(Arith.arithFee(10,1,190.498,0.02)));
//        System.out.println(Arith.getStrBigDecimal(Arith.arithFee(10,1,190.543,0.03)));

        /*
        季度想开空单，周想开多单。 比对季度买入价格与次周卖出价格的基差。
        季度想开多单，周想开空单。 比对季度卖出价格与次周买入价格的基差。
         */
//        开空 190.498  开多190.483

//


//        System.out.println(Arith.div(a,b));
//        System.out.println(Arith.div(a,b,6));
//        System.out.println(Arith.round(a,6));
//        System.out.println(Arith.getStrBigDecimal(a));
//        BigDecimal c = new BigDecimal(a);
//        System.out.println(c.doubleValue());
//        System.out.println(Arith.div(191.46,0.823,5));
//        System.out.println(Arith.compareNum(0.002,0.002));
//        System.out.println(Arith.getStrBigDecimal(Arith.sub(0.0007194,0.0005)));
        bei_tou(100,0.06,10000);
//        System.out.println(get_volume(3));
    }

    public static int get_volume(int num) {
        int i=1;
        int q=0,w=0,j=100;
        while(i<=num){
            q=w+j;

            j=w;
            w=q;
            i++;
        }
        return q;
    }

    private static int count_num = 1;
    public static void bei_tou(int n,double stop_percent,double allPrice) {
        //总共40000，一张合约价值70， 第一次开仓n张, 第二次开仓2n张。 损失x平仓，共可以开仓多少次？设总共可以开仓y次。
        int count = 1;
        for (int i = 0; true; i++) {
            count_num += count;
            int count_volume = get_volume(count_num);
            allPrice = allPrice - (70 * count_volume * stop_percent);
            String log = "第%s次开仓,开仓张数%s,剩余金额%s";
            log = String.format(log,count_num,count_volume,allPrice);
            System.out.println(log);
            if (allPrice > (70 * count_num)) {
                System.out.println("=============================================");
                bei_tou(n,stop_percent,allPrice);
                break;
            }
            System.exit(0);
        }

    }

    public static void get_per(float pre_percent, float now_percent) {
        /**
         * 减仓 or 加仓
         * (加仓) = （当前基差 - 上次基差）> 第二次及之后开仓基差百分比
         * (减仓) = （当前基差 - 上次基差）> 第二次及之后开仓基差百分比
         *
         */
//        System.out.println("==="+Math.abs(now_percent - pre_percent));
        if (position_num == 0)
        {
            if (now_percent > basis_percent_after) {
                System.out.println("f开仓"+now_percent);
                position_num += 1;
            }else{
                System.out.println("不做处理"+now_percent);
            }
        }else if(position_num >= 1) {
            float flag_percent = now_percent - pre_percent;
            if (now_percent < basis_close_percent) {
                position_num = 0;
                System.out.println("清仓1+"+now_percent);
                System.out.println(position_num);
                System.out.println("============================");
                return;
            }
            System.out.println(flag_percent);
            if (Math.abs(flag_percent) > basis_percent_after) {
                if (flag_percent > 0)
                {
                    position_num += 1;
                    System.out.println(position_num);
                    System.out.println("++已经加仓"+now_percent);
                    System.out.println("============================");
                    return;
                }else if (flag_percent < 0) {
                    System.out.println("已经减仓"+now_percent);
                    position_num -= 1;
                    System.out.println(position_num);
                    System.out.println("============================");
                    return;
                }else {
                    System.out.println("--不做处理"+now_percent);
                    return;
                }
            }
            System.out.println("-不做处理"+now_percent);
        }
        System.out.println("开仓次数"+position_num);
        System.out.println("============================");

        //加仓
//        if (flag_percent > 0)
//        {
//            if (flag_percent > basis_percent_after) {
//                position_num += 1;
//                System.out.println("已经加仓");
//            }else{
//                System.out.println("不能加仓");
//            }
//        }else if (flag_percent < 0) {
//            if (Math.abs(flag_percent) > basis_percent_after) {
//                System.out.println("已经减仓");
//                position_num -= 1;
//            }else{
//                System.out.println("不能减仓");
//            }
//        }
    }
}
