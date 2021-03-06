package org.sagebionetworks.bridge.hibernate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.sagebionetworks.bridge.Roles;

public class HibernateAccountTest {
    @Test
    public void attributes() {
        HibernateAccount account = new HibernateAccount();

        // Can set and get attributes.
        Map<String, String> originalAttrMap = new HashMap<>();
        originalAttrMap.put("foo", "foo-value");
        account.setAttributes(originalAttrMap);

        Map<String, String> gettedAttrMap1 = account.getAttributes();
        assertEquals(1, gettedAttrMap1.size());
        assertEquals("foo-value", gettedAttrMap1.get("foo"));

        // Putting values in the map reflect through to the account object.
        gettedAttrMap1.put("bar", "bar-value");

        Map<String, String> gettedAttrMap2 = account.getAttributes();
        assertEquals(2, gettedAttrMap2.size());
        assertEquals("foo-value", gettedAttrMap2.get("foo"));
        assertEquals("bar-value", gettedAttrMap2.get("bar"));

        // Setting attributes to null clears it and returns a new empty map.
        account.setAttributes(null);

        Map<String, String> gettedAttrMap3 = account.getAttributes();
        assertTrue(gettedAttrMap3.isEmpty());

        // Similarly, putting values to the map reflect through.
        gettedAttrMap3.put("baz", "baz-value");

        Map<String, String> gettedAttrMap4 = account.getAttributes();
        assertEquals(1, gettedAttrMap4.size());
        assertEquals("baz-value", gettedAttrMap4.get("baz"));
    }

    @Test
    public void consents() {
        HibernateAccount account = new HibernateAccount();

        // Create dummy consents and keys.
        HibernateAccountConsentKey fooConsentKey = new HibernateAccountConsentKey("foo-guid", 1111);
        HibernateAccountConsentKey barConsentKey = new HibernateAccountConsentKey("bar-guid", 2222);
        HibernateAccountConsentKey bazConsentKey = new HibernateAccountConsentKey("baz-guid", 3333);

        HibernateAccountConsent fooConsent = new HibernateAccountConsent();
        HibernateAccountConsent barConsent = new HibernateAccountConsent();
        HibernateAccountConsent bazConsent = new HibernateAccountConsent();

        // Can set and get.
        Map<HibernateAccountConsentKey, HibernateAccountConsent> originalConsentMap = new HashMap<>();
        originalConsentMap.put(fooConsentKey, fooConsent);
        account.setConsents(originalConsentMap);

        Map<HibernateAccountConsentKey, HibernateAccountConsent> gettedConsentMap1 = account.getConsents();
        assertEquals(1, gettedConsentMap1.size());
        assertSame(fooConsent, gettedConsentMap1.get(fooConsentKey));

        // Putting values in the map reflect through to the account object.
        gettedConsentMap1.put(barConsentKey, barConsent);

        Map<HibernateAccountConsentKey, HibernateAccountConsent> gettedConsentMap2 = account.getConsents();
        assertEquals(2, gettedConsentMap2.size());
        assertSame(fooConsent, gettedConsentMap2.get(fooConsentKey));
        assertSame(barConsent, gettedConsentMap2.get(barConsentKey));

        // Setting to null clears the map. Getting again initializes a new empty map.
        account.setConsents(null);

        Map<HibernateAccountConsentKey, HibernateAccountConsent> gettedConsentMap3 = account.getConsents();
        assertTrue(gettedConsentMap3.isEmpty());

        // Similarly, putting values to the map reflect through.
        gettedConsentMap3.put(bazConsentKey, bazConsent);

        Map<HibernateAccountConsentKey, HibernateAccountConsent> gettedConsentMap4 = account.getConsents();
        assertEquals(1, gettedConsentMap4.size());
        assertSame(bazConsent, gettedConsentMap4.get(bazConsentKey));
    }

    @Test
    public void roles() {
        HibernateAccount account = new HibernateAccount();

        // Can set and get.
        account.setRoles(EnumSet.of(Roles.ADMIN));
        Set<Roles> gettedRoleSet1 = account.getRoles();
        assertEquals(EnumSet.of(Roles.ADMIN), gettedRoleSet1);

        // Putting values in the set reflect through to the account object.
        gettedRoleSet1.add(Roles.DEVELOPER);
        assertEquals(EnumSet.of(Roles.ADMIN, Roles.DEVELOPER), account.getRoles());

        // Setting to null clears the set. Getting again initializes a new empty set.
        account.setRoles(null);
        Set<Roles> gettedRoleSet2 = account.getRoles();
        assertTrue(gettedRoleSet2.isEmpty());

        // Similarly, putting values to the set reflect through.
        gettedRoleSet2.add(Roles.RESEARCHER);
        assertEquals(EnumSet.of(Roles.RESEARCHER), account.getRoles());
    }
}
