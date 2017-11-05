Guillotine - Make your site headless
====================================

[![License](https://img.shields.io/github/license/enonic/lib-sql.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Add Guillotine to your site and get a powerful GraphQL API that enables web-based access all your editorial content. 

Guillotine by default exposes the read-only parts of the Enonic content API, including access to the Enonic query language. More exitingly, Guillotine dynamically analyzes all available content types in the site and automatically generates a GraphQL API specific to your site. This gives you direct, typed and documented access to all content within the site. Including the ability to follow references, child items and access media directly.

The Guillotine API adds a new service to your site in the following path relative to the site root: /_/service/com.enonic.app.guillotine/graphql 

The fastets way to explore the Guillotine API is by adding Guillotine to an existing site (or a demo site from Enonic Market), then install GraphiQL (from Enonic Market) and point it to the site url + service path above. 

| Version | XP Version  | Download |
|---------|-------------| -------- |
| 0.1.5   | 6.10.x      | [Download](http://repo.enonic.com/public/com/enonic/app/guillotine/0.1.5/graphiql-0.1.5.jar) |
