# Google Business Profile API Integration Guide

## Current Status
The restaurant verification system has been prepared for Google Business Profile API integration, but is currently **WAITING FOR API APPROVAL** from Google.

## What's Currently Working
✅ **Google Maps API**: Restaurant search and details retrieval  
✅ **Google OAuth 2.0**: User authentication and access token validation  
✅ **Email Verification System**: Alternative verification methods using available data  
✅ **Admin Verification System**: Manual approval workflows for restaurant ownership  

## What's Waiting for Activation
⏳ **Google Business Profile API**: Direct restaurant ownership verification  
⏳ **Real-time Business Management**: Live verification of restaurant ownership through Google  

## Steps to Activate When API is Approved

### 1. Maven Dependencies
Uncomment the following dependency in `pom.xml`:
```xml
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-mybusinessbusinessinformation</artifactId>
    <version>v1-rev20220201-1.32.1</version>
</dependency>
```

### 2. Service Code Activation
In `RestaurantGoogleVerificationService.java`:

#### A. Main Methods
- Uncomment the Business Profile API code in `getUserManagedRestaurants()` method
- Remove the temporary exception that's currently thrown
- Uncomment the return statement for `managedRestaurants`

#### B. Admin Verification Method
- Uncomment the full implementation in `verifyRestaurantOwnershipViaBusinessAPI()` method
- This will enable 100% confidence verification through official Google data

#### C. Service Initialization
- Uncomment the Business Profile API service initialization commented at the top

### 3. OAuth Scopes Configuration
Ensure the following scopes are properly configured in `application.properties`:
```properties
google.oauth.scopes=https://www.googleapis.com/auth/business.manage,https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/userinfo.profile
```

### 4. Remove Temporary Code
When activated, you can remove:
- The temporary exception throws in `getUserManagedRestaurants()`
- The current limitation messages in admin verification methods
- The fallback email correlation verification (or keep as secondary verification)

## Current Alternative Verification Methods

Since the Business Profile API is not yet available, the system uses:

### 1. Email Correlation Analysis
- **Website Domain Matching**: Compares user email domain with restaurant website
- **Phone Area Verification**: Basic geographic correlation
- **Name Pattern Analysis**: Matches email patterns with restaurant names
- **Previous Claims Check**: Verifies against internal database

### 2. Admin Manual Verification
- **Document Verification**: Request business registration documents
- **Phone Verification**: Manual phone calls to verify ownership
- **Utility Bills**: Request utility bills or lease agreements
- **Google Business Screenshots**: Ask users for Business Profile screenshots

### 3. Confidence Scoring System
- Combines multiple verification methods
- Provides confidence scores (0-100%)
- Recommends approval/rejection based on combined evidence
- Includes admin notes and additional verification steps

## API Activation Checklist

When Google approves the Business Profile API access:

- [ ] Add Maven dependency
- [ ] Uncomment Business Profile API code in service
- [ ] Remove temporary exception throws
- [ ] Test with real Google Business accounts
- [ ] Update admin interface to show API-verified restaurants
- [ ] Add monitoring for API quotas and rate limits
- [ ] Update documentation for users
- [ ] Train admin staff on new verification capabilities

## Security Considerations

### Current (Alternative Methods)
- Limited to publicly available data
- Relies on pattern matching and correlation
- Requires manual admin oversight
- Lower confidence but still secure

### Future (Business Profile API)
- Direct verification through Google's official system
- 100% confidence in ownership verification
- Real-time status checking
- Automatic verification of business profile status

## Testing Strategy

### Current Testing
- Test email correlation algorithms
- Verify admin verification workflows
- Test Google Maps integration
- Validate OAuth token handling

### Future Testing (When API Available)
- Create test Google Business profiles
- Test ownership verification flows
- Verify real-time status updates
- Test error handling for suspended/unverified businesses

## Cost Considerations

### Current Costs
- Google Maps API calls (minimal cost)
- OAuth verification (free)

### Future Costs (When API Available)
- Google Business Profile API calls
- Additional quota management
- Monitor usage and optimize calls

## Support and Documentation

- **Current System**: Email verification and manual admin processes
- **Future System**: Will include official Google Business verification
- **Transition**: Seamless activation when API is approved

## Contact for Activation

When Google Business Profile API access is approved:
1. Follow the activation checklist above
2. Test thoroughly in development environment
3. Deploy to production with monitoring
4. Update user documentation and admin training
