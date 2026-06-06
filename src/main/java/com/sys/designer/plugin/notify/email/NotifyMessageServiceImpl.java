package com.sys.designer.plugin.notify.email;

import com.sys.designer.framework.api.Result;
import com.sys.designer.framework.api.cache.CacheKey;
import com.sys.designer.framework.api.cache.CacheService;
import com.sys.designer.framework.api.plugin.notify.NotifyBaseParam;
import com.sys.designer.framework.api.plugin.notify.NotifyCode;
import com.sys.designer.framework.api.plugin.notify.NotifyMessageService;
import com.sys.designer.framework.api.plugin.notify.NotifyParam;
import com.sys.designer.framework.api.plugin.notify.NotifyResult;
import com.sys.designer.framework.common.cache.KeyParam;
import com.sys.designer.framework.common.config.CommonConfig;
import com.sys.designer.framework.common.errorcode.CommonErrorCode;
import com.sys.designer.framework.common.exception.BusinessRuntimeException;
import com.sys.designer.framework.common.util.ValueUtil;
import jakarta.annotation.Resource;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Component
public class NotifyMessageServiceImpl implements NotifyMessageService {
    @Resource
    private CacheService cacheService;
    @Resource
    private CommonConfig commonConfig;

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    private CacheKey buildCacheKey(NotifyBaseParam param) {
        return KeyParam.of("email-code")
                .express("sms:code:" + param.getCategory() + ":" + param.getSource())
                .addParam("category", param.getCategory())
                .addParam("email", param.getSource());
    }

    private static String randomVerify() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Override
    public NotifyResult send(NotifyParam request) {
        CacheKey keyParam = buildCacheKey(request);
        String code = ValueUtil.isEmpty(request.getCode()) ? randomVerify() : request.getCode();
        EmailMessage message = EmailMessage.of();
        message.addToUser(request.getToUser());
        message.fromUserNickname(request.getFromUserNickname());
        if (ValueUtil.isEmpty(message.fromUserNickname())) {
            String value = commonConfig.getValue("spring.mail.nickname");
            message.fromUserNickname(value);
        }
        message.title(request.getTitle());
        String content = request.getContent();
        if (ValueUtil.isEmpty(content)) {
            content = "<span style=\"font-size:18px;font-weight:400\">您的验证码为:</span><span style=\"color:#9373EE;font-size:24px;font-weight:600\">" + code + "</span>";
        }
        message.content(content, true);
        CompletableFuture.runAsync(() -> sendEmail(message));

        cacheService.setString(keyParam, code);
        NotifyResult smsCode = new NotifyResult();
        smsCode.setCode(code);
        return smsCode;
    }

    private void sendEmail(EmailMessage message) {
        if (!message.isHtml()) {
            return;
        }

        String fromUser = message.fromUser();
        if (ValueUtil.isEmpty(fromUser)) {
            fromUser = commonConfig.getValue("spring.mail.username", true);
        }

        MimeMessage mailMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, message.files().size() > 1);
            if (ValueUtil.isNotEmpty(message.fromUserNickname())) {
                helper.setFrom(new InternetAddress(fromUser, message.fromUserNickname(), "UTF-8"));
            } else {
                helper.setFrom(fromUser);
            }
            helper.setTo(message.toUsers().toArray(new String[0]));
            helper.setSubject(message.title());
            helper.setText(message.content(), true);

            if (ValueUtil.isNotEmpty(message.files())) {
                for (File file : message.files()) {
                    FileSystemResource fileSystemResource = new FileSystemResource(file);
                    String filename = file.getName();
                    helper.addAttachment(filename, fileSystemResource);
                }
            }
            javaMailSender.send(mailMessage);
        } catch (Exception e) {
            throw new BusinessRuntimeException(CommonErrorCode.SERVER_ERROR);
        }
    }

    @Override
    public NotifyCode getCode(NotifyBaseParam param) {
        CacheKey key = buildCacheKey(param);
        Result<String> result = cacheService.getString(key);
        if (!result.isSuccess()) {
            return null;
        }
        if (ValueUtil.isEmpty(result.getResults())) {
            return null;
        }
        NotifyCode smsCode = new NotifyCode();
        smsCode.setCode(result.getResults());
        return smsCode;
    }

    @Override
    public void clearCode(NotifyBaseParam param) {
        this.cacheService.delete(buildCacheKey(param));
    }
}
