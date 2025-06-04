# Halal Guard
Food choices are often influenced by two major factors: religious beliefs and health requirements. In Islamic practice, consuming halal food is a red line that you should not cross at all. However, determining whether a food product is halal is not always easy, especially when shopping in regions where product labeling doesn’t clearly mention halal certification or uses unfamiliar ingredient names.

Similarly, individuals with dietary allergies such as nuts, milk, soy, or gluten need to constantly check food labels to avoid potentially dangerous reactions. For both these groups, shopping becomes a time-consuming and stressful task.

The idea behind our app was simple: use a mobile camera, barcode scanning, and real-time ingredient checking to help users instantly determine if a product is safe — from both a halal and allergen perspective. With the help of external APIs and Firebase-based user profiles, the app delivers a personalized, responsive experience that brings peace of mind and convenience to daily grocery shopping.

## Problem Statement

There are three key issues that inspire this project:

1. **Lack of accessible halal verification tools:**  
   Many food products do not clearly state their halal status, and even when they do, users might not know whether certain ingredients are permissible (e.g., gelatin, emulsifiers, or alcohol-based flavorings). There is no widely available app that provides this information in a fast, user-friendly, and reliable way.

2. **Allergen risk is not personalized in existing apps:**  
   While some apps show ingredients, they often lack the ability to personalize results based on the user’s specific dietary restrictions. As a result, individuals with allergies must manually search for product labels and analyze complex ingredient names on their own.

3. **Manual checking is tedious, error-prone, and slow:**  
   Reading each product label, searching online, and comparing multiple sources can be time-consuming—especially when shopping with limited time or under pressure. This process increases the risk of overlooking harmful ingredients. A solution that automates ingredient checking and filters results dynamically is urgently needed.

## Proposed Solution

To address the problems above, we built a mobile app that allows users to:

- **Scan a product’s barcode** or **search by name** to view its halal status.
- **Check allergen information** against a personalized allergen profile.
- **Save favorite products** for quick future reference.
- **View scan history** of recently scanned or searched items.
- **Share results** as text in other applications.
- **Use a clear visual interface** with icons and labels for easy understanding.
- **Securely log in** and manage personal preferences using Firebase Authentication.

This solution combines both **offline functionality** for saved preferences and **online capability** for real-time ingredient lookups via external APIs. 

The backend logic performs **ingredient-based analysis** by flagging haram or questionable components (such as _"rum"_, _"gelatin"_) and highlights allergens based on user-selected filters. This approach ensures the user receives **instant and accurate feedback** tailored to both their religious and health needs.


