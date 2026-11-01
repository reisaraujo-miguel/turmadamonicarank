package rocks.leug.turmadamonicarank;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import rocks.leug.turmadamonicarank.rank.RankService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, MailTestConfiguration.class})
@Transactional
abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected RecordingEmailSender mail;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RankService rankService;

    @BeforeEach
    void resetMail() {
        mail.clear();
    }

    @AfterEach
    void clearRankCache() {
        rankService.invalidate();
    }
}
