package org.sagebionetworks.bridge.services;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.bridge.dao.ParticipantOption;
import org.sagebionetworks.bridge.dao.ParticipantOption.SharingScope;
import org.sagebionetworks.bridge.dynamodb.OptionLookup;
import org.sagebionetworks.bridge.models.accounts.Account;
import org.sagebionetworks.bridge.models.accounts.HealthId;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.models.studies.StudyParticipant;
import org.sagebionetworks.bridge.models.subpopulations.ConsentSignature;
import org.sagebionetworks.bridge.models.subpopulations.Subpopulation;
import org.sagebionetworks.bridge.models.subpopulations.SubpopulationGuid;
import org.sagebionetworks.bridge.services.email.MimeTypeEmailProvider;
import org.sagebionetworks.bridge.services.email.NotifyOperationsEmailProvider;
import org.sagebionetworks.bridge.services.email.ParticipantRosterProvider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class ParticipantRosterGenerator implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ParticipantRosterGenerator.class);

    private static final Comparator<StudyParticipant> STUDY_PARTICIPANT_COMPARATOR = new Comparator<StudyParticipant>() {
        @Override
        public int compare(StudyParticipant p1, StudyParticipant p2) {
            return p1.getEmail().compareTo(p2.getEmail());
        }
    };

    private final Study study;

    private final Iterator<Account> accounts;

    private final SendMailService sendMailService;
    
    private final HealthCodeService healthCodeService;
    
    private final ParticipantOptionsService optionsService;
    
    private final SubpopulationService subpopService;

    public ParticipantRosterGenerator(Iterator<Account> accounts, Study study, SendMailService sendMailService,
            HealthCodeService healthCodeService, ParticipantOptionsService optionsService,
            SubpopulationService subpopService) {
        this.accounts = accounts;
        this.study = study;
        this.sendMailService = sendMailService;
        this.healthCodeService = healthCodeService;
        this.optionsService = optionsService;
        this.subpopService = subpopService;
    }

    private String getHealthCode(Account account) {
        if (account.getHealthId() != null) {
            HealthId healthId = healthCodeService.getMapping(account.getHealthId());
            if (healthId != null && healthId.getCode() != null) {
                return healthId.getCode();
            }
        }
        return null;
    }
    
    @Override
    public void run() {
        logger.debug("Running participant roster generator...");
        try {
            OptionLookup sharingLookup = optionsService.getOptionForAllStudyParticipants(
                study, ParticipantOption.SHARING_SCOPE);
            OptionLookup emailLookup = optionsService.getOptionForAllStudyParticipants(
                study, ParticipantOption.EMAIL_NOTIFICATIONS);
            OptionLookup externalIdLookup = optionsService.getOptionForAllStudyParticipants(
                study, ParticipantOption.EXTERNAL_IDENTIFIER);
            int count = 0;
            List<StudyParticipant> participants = Lists.newArrayList();
            while (accounts.hasNext()) {
                Account account = accounts.next();

                // Rate limit, so we don't end up consuming DDB as fast as possible. 60 millisecond sleep is
                // ~16.7 accounts/sec. This will allow us to process 60k accounts in an hour.
                try {
                    Thread.sleep(60);
                } catch (InterruptedException ex) {
                    // Interruptions should never happen. Set interrupt status for hygiene.
                    Thread.currentThread().interrupt();
                }

                // If we find no subpopulation names, this user hasn't consented to anything.
                List<String> names = getSubpopulationNames(account);
                if (!names.isEmpty()) {
                    String subpopNames = Joiner.on(", ").join(names);
                    String healthCode = getHealthCode(account);
                    
                    StudyParticipant participant = new StudyParticipant();
                    // Accounts exist that have signatures but no health codes. This may only be from testing, 
                    // but still, do not want roster generation to fail because of this. So we check for this.
                    if (healthCode != null) {
                        SharingScope sharing = sharingLookup.getSharingScope(healthCode);
                        Boolean notifyByEmail = Boolean.valueOf(emailLookup.get(healthCode));
                        participant.setSharingScope(sharing);
                        participant.setNotifyByEmail(notifyByEmail);
                        participant.setExternalId(externalIdLookup.get(healthCode));
                    }
                    participant.setFirstName(account.getFirstName());
                    participant.setLastName(account.getLastName());
                    participant.setEmail(account.getEmail());
                    participant.setSubpopulationNames(subpopNames);
                    for (String attribute : study.getUserProfileAttributes()) {
                        String value = account.getAttribute(attribute);
                        // Whether present or not, add an entry.
                        participant.put(attribute, value);
                    }
                    if (study.isHealthCodeExportEnabled()) {
                        participant.setHealthCode(healthCode);
                    }
                    participants.add(participant);
                    logger.debug("processing account #" + (count++));
                } else {
                    logger.debug("skipping account #" + (count++));
                }
            }
            Collections.sort(participants, STUDY_PARTICIPANT_COMPARATOR);

            MimeTypeEmailProvider roster = new ParticipantRosterProvider(study, participants);
            sendMailService.sendEmail(roster);
            
            String message = "The participant roster for the study '"+study.getName()+"' has been emailed to '"+study.getConsentNotificationEmail()+"'.";
            sendMailService.sendEmail(new NotifyOperationsEmailProvider("A participant roster has been emailed", message));
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            
            String subject = "Generating participant roster failed for the study '"+study.getName()+"'";
            String message = ExceptionUtils.getStackTrace(e);
            sendMailService.sendEmail(new NotifyOperationsEmailProvider(subject, message));
        }
    }
    
    private List<String> getSubpopulationNames(Account account) {
        List<String> names = Lists.newArrayList();
        for (SubpopulationGuid guid : account.getAllConsentSignatureHistories().keySet()) {
            ConsentSignature sig = account.getActiveConsentSignature(guid);
            if (sig != null) {
                // We've found an active consent, get the name of the subpopulation
                Subpopulation subpop = subpopService.getSubpopulation(study, guid);
                names.add(subpop.getName());
            }
        }
        return names;
    }

}
