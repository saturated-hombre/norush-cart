package com.project.norushcart.network;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UrlScraperHelperTest {

    @Test
    public void testDetectPlatform() {
        assertEquals("Blinkit", UrlScraperHelper.detectPlatform("https://blinkit.com/prn/fortune-sunlite-sunflower-oil/prid/32412"));
        assertEquals("Blinkit", UrlScraperHelper.detectPlatform("https://grofers.com/prn/item/prid/123"));
        assertEquals("BigBasket", UrlScraperHelper.detectPlatform("https://www.bigbasket.com/pd/10000150/fresho-apple-royal-gala-regular-4-pcs/"));
        assertEquals("BigBasket", UrlScraperHelper.detectPlatform("https://bbinstant.bigbasket.com/item/123"));
        assertEquals("Zepto", UrlScraperHelper.detectPlatform("https://www.zeptonow.com/pn/amul-taaza-fresh-toned-milk/pvid/91e12762"));
        assertEquals("Swiggy Instamart", UrlScraperHelper.detectPlatform("https://www.swiggy.com/instamart/item/12345"));
        assertEquals("JioMart", UrlScraperHelper.detectPlatform("https://www.jiomart.com/p/groceries/fortune-sunlite-oil/590001234"));
        assertEquals("Amazon", UrlScraperHelper.detectPlatform("https://www.amazon.in/dp/B0CHX1W1XY"));
        assertEquals("Amazon", UrlScraperHelper.detectPlatform("https://amzn.in/d/abcd123"));
        assertEquals("Flipkart", UrlScraperHelper.detectPlatform("https://www.flipkart.com/apple-iphone-15-black-128-gb/p/itm6ac6485515ae4"));
        assertEquals("Flipkart", UrlScraperHelper.detectPlatform("https://fkrt.it/xyz"));
        assertEquals("Myntra", UrlScraperHelper.detectPlatform("https://www.myntra.com/men-tshirts/roadster/roadster-black-tshirt/12345/buy"));
        assertEquals("Ajio", UrlScraperHelper.detectPlatform("https://www.ajio.com/puma-men-sneakers/p/4601234"));
        assertEquals("Nykaa", UrlScraperHelper.detectPlatform("https://www.nykaa.com/kay-beauty-matte-lipstick/p/56789"));
        assertEquals("Croma", UrlScraperHelper.detectPlatform("https://www.croma.com/apple-iphone-15-128gb-black/p/300652"));
        assertEquals("Reliance Digital", UrlScraperHelper.detectPlatform("https://www.reliancedigital.in/apple-iphone-15-128-gb-black/p/493839352"));
        assertEquals("Meesho", UrlScraperHelper.detectPlatform("https://www.meesho.com/s/p/1abcde"));
        assertEquals("Tata CLiQ", UrlScraperHelper.detectPlatform("https://www.tatacliq.com/apple-watch-se/p-mp000000016629938"));
    }

    @Test
    public void testUniversalSlugTitleExtraction() {
        // Blinkit
        String blinkitTitle = UrlScraperHelper.extractTitleFromUrl("https://blinkit.com/prn/fortune-sunlite-refined-sunflower-oil-pouch/prid/32412");
        assertEquals("Fortune Sunlite Refined Sunflower Oil Pouch", blinkitTitle);

        // BigBasket
        String bbTitle = UrlScraperHelper.extractTitleFromUrl("https://www.bigbasket.com/pd/10000150/fresho-apple-royal-gala-regular-4-pcs/");
        assertEquals("Fresho Apple Royal Gala Regular 4 Pcs", bbTitle);

        // Zepto
        String zeptoTitle = UrlScraperHelper.extractTitleFromUrl("https://www.zeptonow.com/pn/amul-taaza-fresh-toned-milk/pvid/91e12762-b9cf-4ca6-a21c-a11be0744719");
        assertEquals("Amul Taaza Fresh Toned Milk", zeptoTitle);

        // Flipkart
        String fkTitle = UrlScraperHelper.extractTitleFromUrl("https://www.flipkart.com/apple-iphone-15-black-128-gb/p/itm6ac6485515ae4");
        assertEquals("Apple Iphone 15 Black 128 Gb", fkTitle);

        // Croma
        String cromaTitle = UrlScraperHelper.extractTitleFromUrl("https://www.croma.com/apple-iphone-15-128gb-black/p/300652");
        assertEquals("Apple Iphone 15 128gb Black", cromaTitle);

        // Reliance Digital
        String rdTitle = UrlScraperHelper.extractTitleFromUrl("https://www.reliancedigital.in/apple-iphone-15-128-gb-black/p/493839352");
        assertEquals("Apple Iphone 15 128 Gb Black", rdTitle);

        // JioMart
        String jioTitle = UrlScraperHelper.extractTitleFromUrl("https://www.jiomart.com/p/groceries/fortune-sunlite-sunflower-oil-1-l/590001234");
        assertEquals("Fortune Sunlite Sunflower Oil 1 L", jioTitle);

        // Myntra
        String myntraTitle = UrlScraperHelper.extractTitleFromUrl("https://www.myntra.com/men-tshirts/roadster/roadster-men-black-cotton-pure-cotton-t-shirt/12345/buy");
        assertEquals("Roadster Men Black Cotton Pure Cotton T Shirt", myntraTitle);

        // Ajio
        String ajioTitle = UrlScraperHelper.extractTitleFromUrl("https://www.ajio.com/puma-men-sneakers/p/4601234");
        assertEquals("Puma Men Sneakers", ajioTitle);

        // Amazon
        String amzTitle = UrlScraperHelper.extractTitleFromUrl("https://www.amazon.in/Apple-iPhone-15-128-GB/dp/B0CHX1W1XY");
        assertEquals("Apple iPhone 15 128 GB", amzTitle);
    }

    @Test
    public void testExtractTitleFromShareText() {
        String bbShare = "Buy Fresho Apple Online at Best Price of Rs 140.00 - bigbasket: https://www.bigbasket.com/pd/10000150/fresho-apple-royal-gala-regular-4-pcs/";
        String cleanUrl = "https://www.bigbasket.com/pd/10000150/fresho-apple-royal-gala-regular-4-pcs/";
        String title = UrlScraperHelper.extractTitleFromShareText(bbShare, cleanUrl);
        assertEquals("Fresho Apple", title);

        String blinkitShare = "Check out Fortune Sunlite Refined Sunflower Oil on Blinkit: https://blinkit.com/prn/fortune-sunlite/prid/123";
        String blinkitTitle = UrlScraperHelper.extractTitleFromShareText(blinkitShare, "https://blinkit.com/prn/fortune-sunlite/prid/123");
        assertEquals("Fortune Sunlite Refined Sunflower Oil", blinkitTitle);

        String zeptoShare = "Found this on Zepto: Amul Butter 500g at ₹275 https://zeptonow.com/pn/amul-butter/pvid/123";
        String zeptoTitle = UrlScraperHelper.extractTitleFromShareText(zeptoShare, "https://zeptonow.com/pn/amul-butter/pvid/123");
        assertEquals("Amul Butter 500g", zeptoTitle);
    }

    @Test
    public void testPlausiblePriceValidation() {
        // High-value item (laptop / macbook) with 500 should be rejected as a coupon/EMI
        assertTrue(!UrlScraperHelper.isPlausiblePrice(500.0, "Apple MacBook Air 13-inch M1"));
        assertTrue(!UrlScraperHelper.isPlausiblePrice(500.0, "HP Pavilion 15 Thin & Light Laptop"));
        assertTrue(!UrlScraperHelper.isPlausiblePrice(499.0, "ASUS Vivobook 15 Laptop"));
        assertTrue(!UrlScraperHelper.isPlausiblePrice(1999.0, "Dell Inspiron 15 Laptop"));

        // High-value item with genuine price (>= 2000) should be accepted
        assertTrue(UrlScraperHelper.isPlausiblePrice(99990.0, "Apple MacBook Air 13-inch M1"));
        assertTrue(UrlScraperHelper.isPlausiblePrice(100000.0, "Lenovo Legion Gaming Laptop"));
        assertTrue(UrlScraperHelper.isPlausiblePrice(45000.0, "Acer Aspire 5 Laptop"));

        // Regular grocery item with smaller price should be valid
        assertTrue(UrlScraperHelper.isPlausiblePrice(135.0, "Fortune Sunlite Refined Sunflower Oil"));
        assertTrue(UrlScraperHelper.isPlausiblePrice(27.0, "Amul Taaza Milk 500ml"));
        assertTrue(UrlScraperHelper.isPlausiblePrice(500.0, "Men Cotton T-Shirt"));
    }

    @Test
    public void testExtractPriceFromTextWithCoupons() {
        // Laptop share text with a ₹500 discount must extract the REAL laptop price (99990), NOT the discount (500)
        String laptopShare1 = "Deal of the day: Apple MacBook Air Laptop at ₹99,990. Save ₹500 with SBI Card https://amzn.in/d/123";
        assertEquals(99990.0, UrlScraperHelper.extractPriceFromText(laptopShare1), 0.01);

        String laptopShare2 = "HP Pavilion Laptop for ₹64,990. Flat ₹500 off on card https://amzn.in/d/456";
        assertEquals(64990.0, UrlScraperHelper.extractPriceFromText(laptopShare2), 0.01);

        String laptopShare3 = "MacBook Pro costing ₹1,00,000. Get ₹500 coupon code https://flipkart.com/...";
        assertEquals(100000.0, UrlScraperHelper.extractPriceFromText(laptopShare3), 0.01);

        // Text that ONLY mentions a coupon or discount should return 0.0, NOT 500.0!
        String onlyCoupon = "Check out this laptop! Flat ₹500 off with code SAVE500 https://amzn.in/d/789";
        assertEquals(0.0, UrlScraperHelper.extractPriceFromText(onlyCoupon), 0.01);

        // Standard grocery item price
        assertEquals(135.0, UrlScraperHelper.extractPriceFromText("Check out on Blinkit at ₹135"), 0.01);
        assertEquals(140.0, UrlScraperHelper.extractPriceFromText("Buy Fresho Apple Online at Best Price of Rs 140.00 - bigbasket"), 0.01);
        assertEquals(27.0, UrlScraperHelper.extractPriceFromText("Milk 500ml at ₹ 27 on Zepto"), 0.01);
    }
}
