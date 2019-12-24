package com.atguigu.gmall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2016101500691640";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDLBQqbEvGHTy7ahZwkAT/sl2v2NahX3FhBCPdTJlvx5TKvQRtlGDzf01uORaIENK1NcxYv21J68O2q3V/t1PLnUtP+Q1FvqdEavGZyYgkkaSmgzCeqHIpKLUlIDod+cJZoUHni0ro27gSu9DID03C9H8rM9tJKX1BkoWsvygM7n0RN0G/VGJgBjwfyZtJOK91JSsDqoqD0WMxDMRvJF05tyJ/Vq1ExwgjPeLBF9H/t1/M1tXkzYINxm/oflL5ebBu9vKhBwCnlaBgIOg67Wo+b3mTEkuLK0oDez0aAUFboNJT2iRk/ItGONkj1iIrTOzNuCfl2Bu3aWB+yyDvL1TWdAgMBAAECggEAeV/ZOF758ShA3boVYF0y53KbxAXOUEDWroL6wkZjn9mkFKy7ARSdiD8nNIai1xrbJTuR3yijRgb2QDm7WUf/tJaLUS7T6AtBjVdndCjySEdq8Mw9RnFrp+8tKZwuG0SaV2ENjbLoS2C/gg1SPYFSnG7+Ys2JaZdUS+VJXLfMeP6rhax9XzFiKGx4v/MzzE+8U5Tgj0vompzo54OWHDyHR3kmsADQRvREq+n7YRQ/yoihzRVj/ZANJa9qHdI+GwQKajgt3b+6TWGRbDrF+Y8xcR5VNUwPBi35iuZ1vEgiyYFw2JujbealQmbV6f2Un3XnhVV9MqKAAAIZm0ErroRIDQKBgQD/uXXfN6gsNY3uhM9nn7GNVsUbYcGpClzr3yUZyb4g46giupC4aArWt/Jn0vS6cQTIzlbkSjOopGSDxxI70qepaFXgY9w0+/MIlKAgcCo0UtMIQyhc61dw03ptzoqFzTLnyGC4JYxPaz5oILJBbi68kOMnvDHnxbCJIZId3FDE4wKBgQDLPQr1AIaHcviNUKJSwGIlbhMRwKXnWPNC+epf1jUfAif5sOMznSiAGPUsIaSWPJoBhhcMpISa8TN3BGXrKcQpsHjR5zJ8YEZfCnVcJNqnKqaVC3QZb9MxLYkm2L2zj+T1Tm5ygkqPA61pI+YqkxoTBQb4WlEYmBZlo9WI962jfwKBgQD1zDzJFoU2IS/QcKWus6eE9D83J4puwcA5E95kYC65DDmjT/Ik3hR0+JAuTW3f+xwfK34Hpm7rDUB0DZewGrX/NoM80J9r6jRbLgiPxWA7tNSVH4AfeDIvB4apHxq0q1zfvxGATs8D01BD5BGSpKaVpypAGjtZzQH3fWYl/4s/AwKBgF2ZIUCwG0Sp+DnslrURkKG4TePxGvauimmf+wZj+lbrgUmG4zvT9uD3nYHThJ8EDdEB1f+mjMNmrdL0HZPtyj+A3EOe/Cs3k3EVufLTrjcqd0lQ2wkT7OlLFUzkqIjXjhRDKK5nvrqacPRo6glVcb6spAcgYRS4higNCbxohHk1AoGAb5zgAcLktvJWAqxkuSmMucLL9Jr98qMap32YfF5pE5KFA+mg2bosfrysJ1ZzNekXyW87cVdQpRwu0eVKGztkvDeQ7Uvr3zKdzU9VFSQ7ZBWpU99rR7/MX/DHXc4YhH8jgNAZrKNFsNkofhnqfkXp0Ea4bfpIO1GLXroeD6lazBY=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAigxEAMPQ6xTYXhQbt1OxBKwXPO0+rH0EOVvbPNGrFS7HPAbHPMm58wOA/7BPn2C+a5aXQMNlWwMHtdR499j029lLtOhjZxHji7dKAjX8d22jYCI5ymBmLmHtXLrCd59yJTHbONnj18/vkZjypLLwUqFX1/z9jfgRndG/ftOfHi+10vNblTafSTt26W/vH+aZ9Jp8Cva7QGhKNrfCsKCXJSzR37PgO4b3cDaBMp4cBN4l6k8tdNQSqwWA+vgVylmY2bMMKaPvabqLBtx/Davg18pGfk+GiNbK9fYN4I8JK/2ksu2V8APrpsU4QdELkhM4VDfEJWCblwd9+5F7AZL8TQIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url;

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url;

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
