package org.sagebionetworks.bridge.hibernate;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Version;

import org.sagebionetworks.bridge.Roles;
import org.sagebionetworks.bridge.models.accounts.AccountStatus;
import org.sagebionetworks.bridge.models.accounts.PasswordAlgorithm;

/** MySQL implementation of accounts via Hibernate. */
// Note: We use a separate class entirely and marshall it to/from GenericAccount instead of using the Account interface
// directly. This is because (1) some of the methods we would need in the Account interface don't really have an
// equivalent in Stormpath, and (2) some of the patterns (especially around embedded collections) don't work really
// well with Hibernate. While not ideal, it was ultimately cleaner to do it this way.
@Entity
@Table(name = "Accounts")
public class HibernateAccount {
    private String id;
    private String studyId;
    private String email;
    private Map<String, String> attributes;
    private Map<HibernateAccountConsentKey, HibernateAccountConsent> consents;
    private Long createdOn;
    private String healthCode;
    private String healthId;
    private Long modifiedOn;
    private String firstName;
    private String lastName;
    private PasswordAlgorithm passwordAlgorithm;
    private String passwordHash;
    private Long passwordModifiedOn;
    private Set<Roles> roles;
    private AccountStatus status;
    private int version;

    /**
     * Account ID, used as a unique identifier for the account that doesn't leak email address (which is personally
     * identifying info).
     */
    @Id
    public String getId() {
        return id;
    }

    /** @see #getId */
    public void setId(String id) {
        this.id = id;
    }

    /** Study ID the account lives in. */
    public String getStudyId() {
        return studyId;
    }

    /** @see #getStudyId */
    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    /** Account email address. */
    public String getEmail() {
        return email;
    }

    /** @see #getEmail */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Map of custom account attributes. Never returns null. */
    @CollectionTable(name = "AccountAttributes", joinColumns = @JoinColumn(name = "accountId",
            referencedColumnName = "id"))
    @Column(name = "attributeValue")
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "attributeKey")
    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    /** @see #getAttributes */
    public void setAttributes(Map<String, String> attributes) {
        // Note: Hibernate doesn't support copying this into a separate map.
        this.attributes = attributes;
    }

    /** Map of consents, keyed by a composite of subpopulation ID and signedOn. Never returns null. */
    @CollectionTable(name = "AccountConsents", joinColumns = @JoinColumn(name = "accountId",
            referencedColumnName = "id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyClass(HibernateAccountConsentKey.class)
    public Map<HibernateAccountConsentKey, HibernateAccountConsent> getConsents() {
        if (consents == null) {
            consents = new HashMap<>();
        }
        return consents;
    }

    /** @see #getConsents */
    public void setConsents(Map<HibernateAccountConsentKey, HibernateAccountConsent> consents) {
        // Note: Hibernate doesn't support copying this into a separate map.
        this.consents = consents;
    }

    /** Epoch milliseconds when the account was created. */
    public Long getCreatedOn() {
        return createdOn;
    }

    /** @see #getCreatedOn */
    public void setCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
    }

    /** Account health code. */
    public String getHealthCode() {
        return healthCode;
    }

    /** @see #getHealthCode */
    public void setHealthCode(String healthCode) {
        this.healthCode = healthCode;
    }

    /** Account health ID, which maps to health code. */
    public String getHealthId() {
        return healthId;
    }

    /** @see #getHealthId */
    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    /** Epoch milliseconds when the account was last modified, including password change. */
    public Long getModifiedOn() {
        return modifiedOn;
    }

    /** @see #getModifiedOn */
    public void setModifiedOn(Long modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    /** User's first name (given name). */
    public String getFirstName() {
        return firstName;
    }

    /** @see #getFirstName */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** User's last name (surname). */
    public String getLastName() {
        return lastName;
    }

    /** @see #getLastName */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * The algorithm used to hash the password.
     *
     * @see PasswordAlgorithm
     */
    @Enumerated(EnumType.STRING)
    public PasswordAlgorithm getPasswordAlgorithm() {
        return passwordAlgorithm;
    }

    /** @see #getPasswordAlgorithm */
    public void setPasswordAlgorithm(PasswordAlgorithm passwordAlgorithm) {
        this.passwordAlgorithm = passwordAlgorithm;
    }

    /** The full password hash, as used by {@link PasswordAlgorithm} to decode it. */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @see #getPasswordHash */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Epoch milliseconds when the user last changed their password. */
    public Long getPasswordModifiedOn() {
        return passwordModifiedOn;
    }

    /** @see #getPasswordModifiedOn */
    public void setPasswordModifiedOn(Long passwordModifiedOn) {
        this.passwordModifiedOn = passwordModifiedOn;
    }

    /**
     * Set of user roles (admin, developer, researcher, etc). Never returns null.
     *
     * @see Roles
     */
    @CollectionTable(name = "AccountRoles", joinColumns = @JoinColumn(name = "accountId", referencedColumnName = "id"))
    @Column(name = "role")
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    public Set<Roles> getRoles() {
        if (roles == null) {
            roles = EnumSet.noneOf(Roles.class);
        }
        return roles;
    }

    /** @see #getRoles */
    public void setRoles(Set<Roles> roles) {
        // Note: Hibernate doesn't support copying this into a separate set.
        this.roles = roles;
    }

    /**
     * Account status (unverified, enabled, disabled.
     *
     * @see AccountStatus
     */
    @Enumerated(EnumType.STRING)
    public AccountStatus getStatus() {
        return status;
    }

    /** @see #getStatus */
    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    @Version
    /** Version number, used by Hibernate to handle optimistic locking. */
    public int getVersion() {
        return version;
    }

    /** @see #getVersion */
    public void setVersion(int version) {
        this.version = version;
    }
}
