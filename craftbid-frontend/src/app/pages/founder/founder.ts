import { Component, OnInit, OnDestroy, Inject, Renderer2 } from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';
import { Topbar } from '../home/topbar/topbar';
import { Navbar } from '../home/navbar/navbar';
import { Footer } from '../home/footer/footer';

@Component({
  selector: 'app-founder',
  standalone: true,
  imports: [CommonModule, RouterLink, Topbar, Navbar, Footer],
  templateUrl: './founder.html',
  styleUrl: './founder.css',
})
export class Founder implements OnInit, OnDestroy {
  founderName = 'Ranjit Barik';
  founderRole = 'Founder & CEO of CraftBid';
  location = 'Bhubaneswar, Odisha, India';
  linkedInUrl = 'https://www.linkedin.com/in/ranjitbarikofficial';
  githubUrl = 'https://github.com/ranjitbarikofficial-cmd';
  email = 'support@craftbid.in';

  quote = 'Every handmade craft has a story. CraftBid gives that story a digital stage.';
  mainDescription =
    "CraftBid was founded by Ranjit Barik with a vision to bring India's talented artisans and handmade crafts into the digital world. CraftBid combines technology, craft reels, and live auctions to help artisans showcase their creativity and connect with customers.";

  imageError = false;

  private jsonLdScriptEl: HTMLScriptElement | null = null;
  private canonicalLinkEl: HTMLLinkElement | null = null;

  techStack = [
    {
      name: 'Spring Boot 3 & Java 21',
      category: 'Backend Architecture',
      icon: '⚡',
      desc: 'High-concurrency RESTful microservices, JPA Hibernate, transaction management, and secure JWT authentication.',
    },
    {
      name: 'WebSocket & STOMP Real-Time',
      category: 'Live Auction Engine',
      icon: '⏱️',
      desc: 'Sub-50ms synchronized 60-second turn countdown timer, dynamic live room participant tracking, and instant bid broadcasting.',
    },
    {
      name: 'Modern Angular & TypeScript',
      category: 'Frontend Engineering',
      icon: '🅰️',
      desc: 'Ultra-fast standalone component architecture, reactive state streams, responsive touch layouts, and dynamic SEO meta tagging.',
    },
    {
      name: 'AWS Cloud & MySQL RDS',
      category: 'Cloud Infrastructure',
      icon: '☁️',
      desc: 'Production AWS EC2 instance with Nginx reverse proxy, automated Let’s Encrypt SSL, and managed AWS RDS MySQL 8.0 database.',
    },
    {
      name: 'Instant Auto-Refund Escrow',
      category: 'Fintech & Security',
      icon: '🛡️',
      desc: '100% risk-free pay-to-bid deposit model. Incremental differential bids with automated refunds for non-winning participants.',
    },
    {
      name: 'Short-Form Craft Reels',
      category: 'Media & Discovery',
      icon: '🎥',
      desc: 'Vertical video streaming engine enabling traditional master craftsmen to show their handmade process and connect emotionally.',
    },
  ];

  milestones = [
    {
      figure: '₹0',
      label: 'Artisan Listing Fees',
      subtext: 'Free for verified traditional creators',
    },
    {
      figure: '90%',
      label: 'Direct Artisan Payout',
      subtext: 'Fair revenue without greedy middlemen',
    },
    {
      figure: '60s',
      label: 'Live Turn Bidding',
      subtext: 'Exciting real-time gamified auctions',
    },
    {
      figure: '100%',
      label: 'Refund Guarantee',
      subtext: 'Instant UPI / bank reversals for deposits',
    },
  ];

  constructor(
    private titleService: Title,
    private metaService: Meta,
    private renderer: Renderer2,
    @Inject(DOCUMENT) private document: Document
  ) {}

  ngOnInit(): void {
    // 1. Set Exact SEO Title
    this.titleService.setTitle(
      "Ranjit Barik – Founder of CraftBid | India's Live Craft Marketplace"
    );

    // 2. Set Exact SEO Meta Description & Tags
    this.metaService.updateTag({
      name: 'description',
      content:
        "Meet Ranjit Barik, founder and developer of CraftBid, India's live handmade craft marketplace connecting artisans and customers through craft reels and live auctions.",
    });

    this.metaService.updateTag({
      name: 'keywords',
      content:
        "Ranjit Barik, CraftBid, Founder of CraftBid, India Handmade Crafts, Live Turn Auctions, Craft Reels, Indian Artisans, Odisha Startup, Tech Entrepreneur, Full Stack Developer, Spring Boot, Angular, AWS",
    });

    this.metaService.updateTag({
      name: 'author',
      content: 'Ranjit Barik',
    });

    this.metaService.updateTag({
      name: 'robots',
      content: 'index, follow',
    });

    // 3. Open Graph Tags
    this.metaService.updateTag({
      property: 'og:title',
      content:
        "Ranjit Barik – Founder of CraftBid | India's Live Craft Marketplace",
    });

    this.metaService.updateTag({
      property: 'og:description',
      content:
        "Meet Ranjit Barik, founder and developer of CraftBid, India's live handmade craft marketplace connecting artisans and customers through craft reels and live auctions.",
    });

    this.metaService.updateTag({
      property: 'og:type',
      content: 'profile',
    });

    this.metaService.updateTag({
      property: 'og:url',
      content: 'https://craftbid.co.in/founder',
    });

    this.metaService.updateTag({
      property: 'og:image',
      content: 'https://craftbid.co.in/ranjit-barik.png',
    });

    this.metaService.updateTag({
      property: 'og:site_name',
      content: 'CraftBid',
    });

    // 4. Twitter Cards
    this.metaService.updateTag({
      name: 'twitter:card',
      content: 'summary_large_image',
    });

    this.metaService.updateTag({
      name: 'twitter:title',
      content:
        "Ranjit Barik – Founder of CraftBid | India's Live Craft Marketplace",
    });

    this.metaService.updateTag({
      name: 'twitter:description',
      content:
        "Meet Ranjit Barik, founder and developer of CraftBid, India's live handmade craft marketplace connecting artisans and customers through craft reels and live auctions.",
    });

    this.metaService.updateTag({
      name: 'twitter:image',
      content: 'https://craftbid.co.in/ranjit-barik.png',
    });

    // 5. Inject Canonical URL
    this.injectCanonicalUrl('https://craftbid.co.in/founder');

    // 6. Inject Structured JSON-LD Schema.org Data
    this.injectStructuredData();
  }

  ngOnDestroy(): void {
    if (this.jsonLdScriptEl && this.jsonLdScriptEl.parentNode) {
      this.renderer.removeChild(this.document.head, this.jsonLdScriptEl);
    }
    if (this.canonicalLinkEl && this.canonicalLinkEl.parentNode) {
      this.renderer.removeChild(this.document.head, this.canonicalLinkEl);
    }
  }

  onImageError(): void {
    this.imageError = true;
  }

  private injectCanonicalUrl(url: string): void {
    let link: HTMLLinkElement | null = this.document.querySelector("link[rel='canonical']");
    if (!link) {
      link = this.renderer.createElement('link');
      this.renderer.setAttribute(link, 'rel', 'canonical');
      this.renderer.appendChild(this.document.head, link);
      this.canonicalLinkEl = link;
    }
    this.renderer.setAttribute(link, 'href', url);
  }

  private injectStructuredData(): void {
    const structuredData = {
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'Person',
          '@id': 'https://craftbid.co.in/founder#person',
          'name': 'Ranjit Barik',
          'givenName': 'Ranjit',
          'familyName': 'Barik',
          'jobTitle': 'Founder & CEO',
          'description':
            "Founder and Developer of CraftBid, India's live handmade craft marketplace connecting artisans and customers through craft reels and live auctions.",
          'url': 'https://craftbid.co.in/founder',
          'image': 'https://craftbid.co.in/ranjit-barik.png',
          'sameAs': [
            'https://www.linkedin.com/in/ranjitbarikofficial',
            'https://github.com/ranjitbarikofficial-cmd',
            'https://craftbid.co.in'
          ],
          'worksFor': {
            '@type': 'Organization',
            '@id': 'https://craftbid.co.in/#organization',
            'name': 'CraftBid',
            'url': 'https://craftbid.co.in'
          },
          'knowsAbout': [
            'Full-Stack Software Engineering',
            'Live Turn Auction Systems',
            'Spring Boot Architecture',
            'Angular & Web Development',
            'WebSocket Real-Time Systems',
            'Indian Traditional Handicrafts',
            'Artisan Empowerment'
          ],
          'address': {
            '@type': 'PostalAddress',
            'addressLocality': 'Bhubaneswar',
            'addressRegion': 'Odisha',
            'addressCountry': 'IN'
          }
        },
        {
          '@type': 'Organization',
          '@id': 'https://craftbid.co.in/#organization',
          'name': 'CraftBid',
          'url': 'https://craftbid.co.in',
          'logo': 'https://craftbid.co.in/CRAFTBID.png',
          'founder': {
            '@id': 'https://craftbid.co.in/founder#person'
          },
          'foundingDate': '2026',
          'description':
            "India's 1st live turn craft marketplace and reels platform empowering verified master artisans.",
          'address': {
            '@type': 'PostalAddress',
            'addressLocality': 'Bhubaneswar',
            'addressRegion': 'Odisha',
            'addressCountry': 'IN'
          }
        },
        {
          '@type': 'WebPage',
          '@id': 'https://craftbid.co.in/founder#webpage',
          'url': 'https://craftbid.co.in/founder',
          'name': "Ranjit Barik – Founder of CraftBid | India's Live Craft Marketplace",
          'description':
            "Meet Ranjit Barik, founder and developer of CraftBid, India's live handmade craft marketplace connecting artisans and customers through craft reels and live auctions.",
          'inLanguage': 'en-US',
          'mainEntity': {
            '@id': 'https://craftbid.co.in/founder#person'
          }
        }
      ]
    };

    this.jsonLdScriptEl = this.renderer.createElement('script');
    this.renderer.setAttribute(this.jsonLdScriptEl, 'type', 'application/ld+json');
    this.jsonLdScriptEl!.textContent = JSON.stringify(structuredData);
    this.renderer.appendChild(this.document.head, this.jsonLdScriptEl);
  }
}
