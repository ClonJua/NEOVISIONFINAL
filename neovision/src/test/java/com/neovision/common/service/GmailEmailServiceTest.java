package com.neovision.common.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Los datos que escriben los usuarios no deben poder inyectar HTML en los correos. */
class GmailEmailServiceTest {

    @Test
    void cancelacion_escapaNombresYMotivo() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        GmailEmailService service = new GmailEmailService(sender);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@neovision.test");
        ReflectionTestUtils.setField(service, "fromName", "NeoVision");

        service.enviarCancelacion("p@mail.com", "<b>Ana</b>",
                "e@uni.edu", "Luis", "d@uni.edu", "Doc",
                "2026-10-08", "08:00", "10:00",
                "Paciente", "<a href='http://malo'>clic</a>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, atLeastOnce()).send(captor.capture());
        MimeMessage msg = captor.getValue();
        msg.saveChanges();
        String html = cuerpo(msg);

        assertThat(html).doesNotContain("<b>Ana</b>").contains("&lt;b&gt;Ana&lt;/b&gt;");
        assertThat(html).doesNotContain("<a href").contains("&lt;a href=");
    }

    private static String cuerpo(MimeMessage msg) throws Exception {
        Object content = msg.getContent();
        if (content instanceof MimeMultipart mp) {
            return buscarTexto(mp);
        }
        return content.toString();
    }

    private static String buscarTexto(MimeMultipart mp) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mp.getCount(); i++) {
            Object c = mp.getBodyPart(i).getContent();
            sb.append(c instanceof MimeMultipart inner ? buscarTexto(inner) : c.toString());
        }
        return sb.toString();
    }
}
