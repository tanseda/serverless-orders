# Source code structure
This project contains source code and supporting files for a serverless application that you can deploy with the SAM CLI. It includes the following files and folders.

##OrdersFunction/
 Code for the application's Lambda function and Project Dockerfile.

 * **com.stan.orders.handler.customers:** This package contains source code for the GetAllOrdersForCustomer lambda function
 * **com.stan.orders.handler.orders:** This package contains source code for the GetAllItemsForOrder lambda function
 * **com.stan.orders.handler.products:** This package contains source code for the GetProduct lambda function
 * **com.stan.orders.handler.repository:** This package contains source code for the DynamoDB integration
 * **order-api-swagger.yml :** This is the REST API Definitions file.

##template.yaml
  The template that defines the application's AWS resources.
  
##events/
  Invocation events that you can use to invoke the function.
