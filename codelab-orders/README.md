### Order Management

Simple order lifecycle management. Gives us a base on which we can then perform scaling exercises within our modular 
monolith. Pretty useless till IAM is added, but hey, at least we'll have some sample data.

### Use cases
* Create products
* Create customers
* Create orders with one or more line items
* Update order status

### Conventions
* Denormalized tables for all entities
* Embedded ids instead of id class for composite ids like LineItemId
* DTOs defined per _action_
