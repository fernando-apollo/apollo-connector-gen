package com.apollographql.oas.select;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Recordings {

  public final String[] fromFile(final String file) throws IOException {
    final Path filePath = Path.of(file);
    List<String> linesList = Files.readAllLines(filePath);
    return linesList.toArray(new String[0]);
  }

  public final String[] fromResource(final String resourceName) throws IOException {
    InputStream input = Recordings.class.getClassLoader().getResourceAsStream(resourceName);
    assert input != null;

    return fromInputStream(input);
  }

  public static String[] fromInputStream(final InputStream input) {
    List<String> linesList = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      String line;
      while ((line = reader.readLine()) != null) {
        linesList.add(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    // Convert List to Array
    return linesList.toArray(new String[0]);
  }

  static final String[] TMF633_IntentOrValue_UNION = new String[]{
    "n", /*    visit '/product'? */
    "y", /*    visit '/product/{id}'? */
    "n", /* Add all properties from Extensible?:
 - @baseType: String,
 - @schemaLocation: String,
 - @type: String
 */
    "n", /*        add property '@baseType: String'? */
    "n", /*        add property '@schemaLocation: String'? */
    "n", /*        add property '@type: String'? */
    "n", /* Add all properties from Addressable?:
 - href: String,
 - id: String
 */
    "n", /*        add property 'href: String'? */
    "n", /*        add property 'id: String'? */
    "n", /* Add all properties from Product?:
 - agreementItem: [AgreementItemRef],
 - billingAccount: BillingAccountRef,
 - creationDate: String,
 - description: String,
 - intent: IntentRefOrValue,
 - isBundle: Boolean,
 - isCustomerVisible: Boolean,
 - name: String,
 - orderDate: String,
 - place: [RelatedPlaceRefOrValue],
 - product: [ProductRefOrValue],
 - productCharacteristic: [Characteristic],
 - productOffering: ProductOfferingRef,
 - productOrderItem: [RelatedOrderItem],
 - productPrice: [ProductPrice],
 - productRelationship: [ProductRelationship],
 - productSerialNumber: String,
 - productSpecification: ProductSpecificationRef,
 - productTerm: [ProductTerm],
 - realizingResource: [ResourceRef],
 - realizingService: [ServiceRef],
 - relatedParty: [RelatedPartyOrPartyRole],
 - startDate: String,
 - status: ProductStatusType,
 - terminationDate: String
 */
    "n", /*     add property 'agreementItem: [AgreementItemRef]'? */
    "n", /*     add property 'billingAccount: BillingAccountRef'? */
    "n", /*     add property 'creationDate: String'? */
    "y", /*     add property 'description: String'? */
    "y", /*     add property 'intent: IntentRefOrValue'? */
    "n", /*     add property 'isBundle: Boolean'? */
    "n", /*     add property 'isCustomerVisible: Boolean'? */
    "n", /*     add property 'name: String'? */
    "n", /*     add property 'orderDate: String'? */
    "n", /*     add property 'place: [RelatedPlaceRefOrValue]'? */
    "n", /*     add property 'product: [ProductRefOrValue]'? */
    "n", /*     add property 'productCharacteristic: [Characteristic]'? */
    "n", /*     add property 'productOffering: ProductOfferingRef'? */
    "n", /*     add property 'productOrderItem: [RelatedOrderItem]'? */
    "n", /*     add property 'productPrice: [ProductPrice]'? */
    "n", /*     add property 'productRelationship: [ProductRelationship]'? */
    "n", /*     add property 'productSerialNumber: String'? */
    "n", /*     add property 'productSpecification: ProductSpecificationRef'? */
    "n", /*     add property 'productTerm: [ProductTerm]'? */
    "n", /*     add property 'realizingResource: [ResourceRef]'? */
    "n", /*     add property 'realizingService: [ServiceRef]'? */
    "n", /*     add property 'relatedParty: [RelatedPartyOrPartyRole]'? */
    "n", /*     add property 'startDate: String'? */
    "n", /*     add property 'status: ProductStatusType'? */
    "n", /*     add property 'terminationDate: String'? */
    "n", /* Add all properties from EntityRef?:
 - @referredType: String,
 - href: String,
 - id: String,
 - name: String
 */
    "n", /*             add property '@referredType: String'? */
    "n", /*             add property 'href: String'? */
    "n", /*             add property 'id: String'? */
    "n", /*             add property 'name: String'? */
    "n", /* Add all properties from Intent?:
 - attachment: [AttachmentRefOrValue],
 - characteristic: [Characteristic],
 - context: String,
 - creationDate: String,
 - description: String,
 - expression: Expression,
 - intentRelationship: [EntityRelationship],
 - intentSpecification: EntityRef,
 - isBundle: Boolean,
 - lastUpdate: String,
 - lifecycleStatus: String,
 - name: String,
 - priority: String,
 - relatedParty: [RelatedPartyRefOrPartyRoleRef],
 - statusChangeDate: String,
 - validFor: TimePeriod,
 - version: String
 */
    "n", /*           add property 'attachment: [AttachmentRefOrValue]'? */
    "n", /*           add property 'characteristic: [Characteristic]'? */
    "n", /*           add property 'context: String'? */
    "n", /*           add property 'creationDate: String'? */
    "n", /*           add property 'description: String'? */
    "n", /*           add property 'expression: Expression'? */
    "n", /*           add property 'intentRelationship: [EntityRelationship]'? */
    "n", /*           add property 'intentSpecification: EntityRef'? */
    "n", /*           add property 'isBundle: Boolean'? */
    "n", /*           add property 'lastUpdate: String'? */
    "n", /*           add property 'lifecycleStatus: String'? */
    "y", /*           add property 'name: String'? */
    "y", /*           add property 'priority: String'? */
    "n", /*           add property 'relatedParty: [RelatedPartyRefOrPartyRoleRef]'? */
    "n", /*           add property 'statusChangeDate: String'? */
    "n", /*           add property 'validFor: TimePeriod'? */
    "n", /*           add property 'version: String'? */
  };

  public static final String[] TMF633_RefAndUnion = new String[]{
    "n", /*    visit '/product'? */
    "y", /*    visit '/product/{id}'? */
    "y", /* Add all properties from Extensible?:
 - @baseType: String,
 - @schemaLocation: String,
 - @type: String
 */
    "y", /* Add all properties from Addressable?:
 - href: String,
 - id: String
 */
    "n", /* Add all properties from Product?:
 - agreementItem: [AgreementItemRef],
 - billingAccount: BillingAccountRef,
 - creationDate: String,
 - description: String,
 - intent: IntentRefOrValue,
 - isBundle: Boolean,
 - isCustomerVisible: Boolean,
 - name: String,
 - orderDate: String,
 - place: [RelatedPlaceRefOrValue],
 - product: [ProductRefOrValue],
 - productCharacteristic: [Characteristic],
 - productOffering: ProductOfferingRef,
 - productOrderItem: [RelatedOrderItem],
 - productPrice: [ProductPrice],
 - productRelationship: [ProductRelationship],
 - productSerialNumber: String,
 - productSpecification: ProductSpecificationRef,
 - productTerm: [ProductTerm],
 - realizingResource: [ResourceRef],
 - realizingService: [ServiceRef],
 - relatedParty: [RelatedPartyOrPartyRole],
 - startDate: String,
 - status: ProductStatusType,
 - terminationDate: String
 */
    "n", /*     add property 'agreementItem: [AgreementItemRef]'? */
    "y", /*     add property 'billingAccount: BillingAccountRef'? */
    "n", /*     add property 'creationDate: String'? */
    "y", /*     add property 'description: String'? */
    "y", /*     add property 'intent: IntentRefOrValue'? */
    "y", /*     add property 'isBundle: Boolean'? */
    "n", /*     add property 'isCustomerVisible: Boolean'? */
    "y", /*     add property 'name: String'? */
    "y", /*     add property 'orderDate: String'? */
    "n", /*     add property 'place: [RelatedPlaceRefOrValue]'? */
    "n", /*     add property 'product: [ProductRefOrValue]'? */
    "n", /*     add property 'productCharacteristic: [Characteristic]'? */
    "n", /*     add property 'productOffering: ProductOfferingRef'? */
    "n", /*     add property 'productOrderItem: [RelatedOrderItem]'? */
    "n", /*     add property 'productPrice: [ProductPrice]'? */
    "n", /*     add property 'productRelationship: [ProductRelationship]'? */
    "y", /*     add property 'productSerialNumber: String'? */
    "n", /*     add property 'productSpecification: ProductSpecificationRef'? */
    "n", /*     add property 'productTerm: [ProductTerm]'? */
    "n", /*     add property 'realizingResource: [ResourceRef]'? */
    "n", /*     add property 'realizingService: [ServiceRef]'? */
    "n", /*     add property 'relatedParty: [RelatedPartyOrPartyRole]'? */
    "y", /*     add property 'startDate: String'? */
    "n", /*     add property 'status: ProductStatusType'? */
    "y", /*     add property 'terminationDate: String'? */
    "y", /* Add all properties from EntityRef?:
 - @referredType: String,
 - href: String,
 - id: String,
 - name: String
 */
    "y", /* Add all properties from BillingAccountRef?:
 - ratingType: String
 */
    "n", /* Add all properties from Intent?:
 - attachment: [AttachmentRefOrValue],
 - characteristic: [Characteristic],
 - context: String,
 - creationDate: String,
 - description: String,
 - expression: Expression,
 - intentRelationship: [EntityRelationship],
 - intentSpecification: EntityRef,
 - isBundle: Boolean,
 - lastUpdate: String,
 - lifecycleStatus: String,
 - name: String,
 - priority: String,
 - relatedParty: [RelatedPartyRefOrPartyRoleRef],
 - statusChangeDate: String,
 - validFor: TimePeriod,
 - version: String
 */
    "n", /*           add property 'attachment: [AttachmentRefOrValue]'? */
    "n", /*           add property 'characteristic: [Characteristic]'? */
    "y", /*           add property 'context: String'? */
    "y", /*           add property 'creationDate: String'? */
    "y", /*           add property 'description: String'? */
    "n", /*           add property 'expression: Expression'? */
    "n", /*           add property 'intentRelationship: [EntityRelationship]'? */
    "n", /*           add property 'intentSpecification: EntityRef'? */
    "y", /*           add property 'isBundle: Boolean'? */
    "n", /*           add property 'lastUpdate: String'? */
    "y", /*           add property 'lifecycleStatus: String'? */
    "y", /*           add property 'name: String'? */
    "y", /*           add property 'priority: String'? */
    "n", /*           add property 'relatedParty: [RelatedPartyRefOrPartyRoleRef]'? */
    "n", /*           add property 'statusChangeDate: String'? */
    "y", /*           add property 'validFor: TimePeriod'? */
    "y", /*           add property 'version: String'? */
    "y", /* Add all properties from TimePeriod?:
 - endDateTime: String,
 - startDateTime: String
 */
  };
}
