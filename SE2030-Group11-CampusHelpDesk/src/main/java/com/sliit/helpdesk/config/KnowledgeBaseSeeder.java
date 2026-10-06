package com.sliit.helpdesk.config;

// Knowledge Base Seeder is part of the campus help desk config code.

import com.sliit.helpdesk.auth.model.User;
import com.sliit.helpdesk.auth.repository.UserRepository;
import com.sliit.helpdesk.knowledgebase.model.KbArticle;
import com.sliit.helpdesk.knowledgebase.model.KbCategory;
import com.sliit.helpdesk.knowledgebase.repository.ArticleRepository;
import com.sliit.helpdesk.knowledgebase.repository.KbCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(2)
public class KnowledgeBaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseSeeder.class);

    private final KbCategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public KnowledgeBaseSeeder(
            KbCategoryRepository categoryRepository,
            ArticleRepository articleRepository,
            UserRepository userRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        User author = userRepository.findByEmailIgnoreCase("it.support@sliit.lk")
                .or(() -> userRepository.findByEmailIgnoreCase("admin@sliit.lk"))
                .or(() -> userRepository.findByEmailIgnoreCase(TestUserSeeder.TEST_EMAIL))
                .or(() -> userRepository.findAll().stream().findFirst())
                .orElse(null);
        if (author == null) {
            log.warn("Skipped knowledge-base seed because no users exist yet");
            return;
        }

        Map<String, KbCategory> categories = new LinkedHashMap<>();
        for (String name : new String[] {
                "Accounts & access",
                "IT Infrastructure",
                "Fees & payments",
                "Campus facilities",
                "Hostel facilities",
                "Academic records",
                "Library services"
        }) {
            categories.put(name, ensureCategory(name));
        }

        int created = 0;
        created += article(author, categories.get("Accounts & access"), 1420,
                "Resetting your SLIIT Network & LMS Credentials",
                """
                Use this guide when CourseWeb, the student portal, or campus Wi-Fi rejects your password.

                1. Open https://account.sliit.lk and choose Forgot password.
                2. Enter your university email (@sliit.lk or @my.sliit.lk). The reset link expires in 30 minutes.
                3. Check junk/spam if the mail does not arrive within five minutes.
                4. After resetting, sign out of CourseWeb completely, then sign in again with the new password.
                5. Forget the eduroam / SLIIT-WiFi network on your device and reconnect so the new password is stored.

                If the portal still shows invalid credentials, open an IT Support ticket with your student ID and a screenshot of the error.
                """);
        created += article(author, categories.get("IT Infrastructure"), 2100,
                "Connecting to SLIIT Campus Wi-Fi & Eduroam VPN",
                """
                Malabe campus wireless uses eduroam. Use your full university email, not just the username.

                Windows / macOS
                • Choose the eduroam network.
                • Username: your full email (for example student@sliit.lk).
                • Password: your campus password.
                • Accept the SLIIT certificate when prompted.

                Mobile
                • Android: EAP method PEAP, Phase 2 MSCHAPv2, CA certificate Use system certificates, domain sliit.lk.
                • iPhone: join eduroam, trust the certificate, then keep Wi-Fi Assist off in lecture halls if the session drops.

                VPN (off-campus library journals)
                Install the campus AnyConnect / Eduroam VPN profile from the IT site, then sign in with the same email. If authentication loops, forget the network and retry, or raise an IT Support ticket.
                """);
        created += article(author, categories.get("Fees & payments"), 920,
                "Understanding Semester Payment Breakdowns & Slips",
                """
                Official payment proof lives in the student portal, not in email screenshots.

                1. Sign in to the student portal and open Finance → Payment history.
                2. Download the VAT receipt for the semester you need. Embassy desks reject self-printed portal screenshots.
                3. Bank transfers can take two working days to clear. If the portal still shows Pending after that, submit a Finance ticket with the payment date, amount, and bank reference.
                4. Repeaters and late-registration penalties appear as separate line items — they are not included in the standard semester fee.

                Need a sealed original for a visa? Open a Finance ticket and collect it from the Finance counter at Block A.
                """);
        created += article(author, categories.get("Hostel facilities"), 640,
                "Submitting Room Maintenance & Repair Requests",
                """
                Hostel repairs are routed to Estate / Facilities. Photos speed up the visit.

                Include in your ticket
                • Hostel name and room number (for example Block C / C-204).
                • Whether the issue is plumbing, electrical, furniture, or door locks.
                • When it started and whether it is a safety risk (flooding, exposed wiring, no lighting in corridors).

                Photograph the fault in daylight if you can. Urgent water leaks or electrical sparks should also be called to Facilities on 011-754-4810 while the ticket is open. Do not wait for a portal reply for flooding.
                """);
        created += article(author, categories.get("Academic records"), 1100,
                "Checking Exam Timetables and Module Registration Holds",
                """
                Exam halls and clash reports are published on CourseWeb, not on this help desk.

                • Open CourseWeb → Faculty notices for the draft timetable, then the final hall list about a week before exams.
                • Registration holds (fees, library, or academic warning) block add/drop. Clear Finance or Library holds first, then ask the Faculty Office.
                • Module clashes: capture both timetables and submit an Academic ticket with your registration number and intake.

                Transcripts and degree letters are issued by the Faculty Office. Allow three working days in semester and five days during exams.
                """);
        created += article(author, categories.get("Library services"), 480,
                "Booking Discussion Rooms and Managing Printing Credits",
                """
                Group study rooms and printers are managed by the library system.

                Discussion rooms
                Book online from the library kiosk or portal. Rooms are 60–120 minutes, two bookings per student per day. Bring student ID to collect the key.

                Printing
                Top up printer quotas at the library counter or via the student portal wallet. Jobs sent to Follow-Me Print release at any library printer with your ID card. Unreleased jobs expire after 24 hours.

                Overdue books create a Library hold that can block exam admission — return or renew before the due date.
                """);
        created += article(author, categories.get("Campus facilities"), 180,
                "Report a classroom fault",
                """
                Note the building, room number, and whether the issue is electrical, furniture, or AV equipment.
                Submit a Facilities ticket with a photo if possible. Projectors, broken boards, and missing HDMI cables should mention the module and the next lecture time so staff can attend before class.
                Urgent safety issues should also be called in to Facilities on 011-754-4810.
                """);
        created += article(author, categories.get("Accounts & access"), 42,
                "Reset your campus password",
                """
                Go to account.sliit.lk, choose Forgot password, and use your student email. The reset link expires in 30 minutes.
                If the email does not arrive, check the spam folder and then open an IT Support ticket with your student ID.
                """);
        created += article(author, categories.get("Academic records"), 24,
                "Request an official transcript",
                """
                Transcripts are issued by the Faculty Office, not IT. Submit an Academic ticket with your registration number, intake, and whether you need a digital or sealed paper copy.
                Allow three working days during the semester and five days during exams.
                """);
        created += article(author, categories.get("Fees & payments"), 37,
                "Download a fee payment receipt",
                """
                Sign in to the student portal, open Finance, and choose Payment history.
                Official VAT receipts for visas must be requested through a Finance ticket with the payment date and bank reference.
                Self-printed screenshots are not accepted by the embassy desk.
                """);

        if (created > 0) {
            log.info("Seeded {} knowledge-base articles", created);
        }
    }

    private KbCategory ensureCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            KbCategory category = new KbCategory();
            category.setName(name);
            return categoryRepository.save(category);
        });
    }

    private int article(User author, KbCategory category, int views, String title, String content) {
        if (articleRepository.existsByTitleIgnoreCase(title)) {
            return 0;
        }
        KbArticle article = new KbArticle();
        article.setTitle(title);
        article.setContent(content.trim());
        article.setCategory(category);
        article.setAuthor(author);
        article.setViewCount(views);
        articleRepository.save(article);
        return 1;
    }
}
