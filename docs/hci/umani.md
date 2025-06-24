# Umani

> A self-hosted, web-based analytics application built with a Node.js (Next.js) frontend and PostgreSQL (or optionally MySQL) as a relational backend. It provides privacy-respecting, JavaScript-based tracking of user interactions, and exposes a RESTful API for data ingestion and visualization. Umami operates as a stateless application server, using environment-driven configuration and JWT-based authentication, and it is typically deployed via containerized environments (e.g., Docker) with persistent storage bound to the database volume. The application adheres to modern DevOps conventions including 12-factor app principles, enabling declarative configuration, horizontal scalability, and infrastructure-as-code compatibility.

## Install

```bash
git clone https://github.com/umami-software/umami.git
docker compose up
```

## User

- **Username**: admin
- **Password**: umami

## Features

Umami is an open-source, privacy-focused web analytics platform designed as a lightweight alternative to tools like Google Analytics. Its user interface (UI) emphasizes simplicity and clarity, providing essential insights without overwhelming users. Here's an overview of Umami's key UI and analytics features:([umami.is][1])

### 🔍 Core Analytics Features

* **Page Views & Unique Visitors**: Track the number of page views and unique visitors to your website.

* **Referrers**: Identify which external sources are directing traffic to your site.

* **Device & Browser Information**: Gain insights into the devices and browsers your visitors are using.([simpleanalytics.com][2])

* **Geolocation**: Understand where your visitors are coming from geographically.

* **Custom Event Tracking**: Monitor specific user interactions, such as button clicks or form submissions, by defining custom events. ([xtom.com][3])

* **UTM Parameter Tracking**: Analyze the effectiveness of marketing campaigns by tracking UTM parameters in URLs. ([xtom.com][3])

### 🎯 Advanced Features

* **Segments & Filters**: Dive deeper into your data by creating segments and applying filters to analyze specific subsets of your audience. ([umami.is][4])

* **Funnels**: Visualize user journeys and understand conversion paths by setting up funnels. ([umami.is][4])

* **Retention Analysis**: Measure how well your website retains visitors over time.&#x20;

### 🛠️ Deployment Options

* **Self-Hosting**: Umami can be self-hosted, giving you full control over your data. It supports deployment via Docker and can be hosted on various platforms like Vercel, Netlify, Railway, and fly.io. ([medium.com][5])

* **Cloud Hosting**: Alternatively, you can use Umami's cloud-hosted version, which offers a free tier for smaller projects. ([medium.com][5])

### 🧩 Additional Features

* **Team Collaboration**: Share access with team members while maintaining control over your data. ([xtom.com][3])

* **Multi-Language Support**: The UI is translated into multiple languages, making it accessible to a global audience. ([medium.com][5])

* **Dark Mode**: Umami offers a dark mode for users who prefer a darker interface. ([medium.com][5])

## References

- [Umami](https://github.com/umami-software/umami)
