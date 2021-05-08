# Online Print Orders Service API

The purpose of this API is to expose orders details with the following queries:
* Get the price and name for a given product
* Get all the items for a given order
* Get all the orders for a given customer

## Assumptions
1. An order record can not contain duplicate products.

## System Architecture
In order to achieve our purpose with minimum maintenance effort, I decided to choose a serverless solution. The following advantages led me come up with this architecture.
- no server management required
- automatically scale from zero to peak demands
- lower cost
- consistent performance with AWS Serverless Platform have built-in fault tolerance and availability

![Orders Table](images/system-design.png)

### API Gateway:
WhenThe API Gateway invoked with a REST request, it will proxy the related Lambda Function according to the path URI. <br/>

### Security:
I preferred IAM authentication for the APIs in the API Gateway. API Gateway invokes the API route only if the client has execute-api permission for the route.<br/>
The Lambda Functions will only have an execution role with an IAM policy that authorizes to Read/Query from DynamoDB table and upload its log to The CloudWatch. <br/>
Samples managed policy are given below:
- **AWSLambdaBasicExecutionRole** – Upload logs to CloudWatch
- **AWSLambdaDynamoDBExecutionRole** – Read from DynamoDB Streams

### Logging & Monitoring:
CloudWatch Logs:
* AWS Lambda execution logs will be stored in AWS CloudWatch Logs

CloudWatch Metrics:
* AWS Lambda and DynamoDB metrics will be displayed in AWS CloudWatch Metrics such as
* Invocations, Durations, Concurrent Executions
* Error count, Success Rates, Throttles
* ConsumedReadCapacityUnits, ThrottledRequests, etc. 


### Data storage
Example dataset is given below

| customerId  |  orderRef  |       productId         | price  |  productName  |
|-------------|------------|-------------------------|--------|---------------|
|     56789   |    12345   | notebook_hardcover_red  |  19.99 |  Red Notebook |
|     56789   |    12345   | notebook_hardcover_blue |  18.99 | Blue Notebook |

According to the example dataset and the required queries, access pattern candidates are defined below:

|           Access Patterns                            |  Query Conditions                             |
|------------------------------------------------------|-----------------------------------------------|
| Get the price and name for a given product           | Primary Key on Table, ID="PRODUCT-productId"  |
| Get all the items for a given order                  | Primary Key on Table, ID="CUSTOMER-12345"     |
| Get all the orders for a given customer              | Primary Key on Table, ID="ORDER-12345"        |

Based on the access patterns and query conditions, the following table is designed:

![Orders Table](images/table.png)

The table has a compound primary key (partition + sort key)

This solution has 3 types of partition keys which are **PRODUCT**, **CUSTOMER** and **ORDER**.
The format of partition key is "\<KEYWORD\>-\<ID\>". The keywords represent the partition key type to avoid conflicts in IDs.
For example, for the customer 56789, the partition key will be "CUSTOMER-56789".

The sort key is a string, and it will differ for each type of partition key.

| partition key   |  sorting key     |
|-----------------|------------------|
| CUSTOMER-\<ID\> |    <OrderRef>    |
|   ORDER-\<ID\>  |    <ProductId>   |
| PRODUCT-\<ID\>  |   <ProductName>  |

In the Orders table:<br/>

![Orders Table](images/customer-row.png)

Row 1: The item contains the order with orderRef 12345 for customer 56789.<br/>
    Partition key is *CUSTOMER-56789* and sort key is the *OrderRef*. As data attributes, there is a list of products that are associated with the order.

![Orders Table](images/orders-row.png)

Row 2: The item is storing the details of product notebook_hardcover_red in the order with orderRef 12345.<br/>
Row 3: The item is storing the details of product notebook_hardcover_blue in the order with orderRef 12345.<br/>
    Partition key is *ORDER-12345* and sort key is the *ProductId*. The data attributes are details about the product in the order.
  
![Orders Table](images/products-row.png)

Row 4: The item is storing the details of product notebook_hardcover_blue.
Row 5: The item is storing the details of product notebook_hardcover_red.
    Partition key is *PRODUCT-notebook_hardcover_red* and sort key is the *ProductName*. 
    
### REST APIs
Please see OrdersFunction/order-api-swagger.yml for the detailed template of REST APIs.

### Lambda Functions
There will be separate lambda functions for each queries:

#### Get the price and name for a given product

The API Gateway will proxy the request with the following URI to the *GetProductsFunction* :
```
    /products/{productId}
```
Sample response body is:

```
{
    "productId": "notebook_hardcover_blue",
    "productName": "Blue Book",
    "price": 18.9
}
``` 

The retrieve query: 
```java
QueryRequest queryReq = new QueryRequest(TABLE_NAME);

Map<String, AttributeValue> attrValues;
attrValues.put(":v_id", new AttributeValue("PRODUCT-"+productId));

queryReq.setKeyConditionExpression("par = :v_id");
queryReq.setExpressionAttributeValues(attrValues);

List<Map<String, AttributeValue>> result = client.query(queryReq).getItems();

return result.stream()
       .map(ProductMapper::map)
       .findFirst();
```

#### Get all the items for a given order

The API Gateway will proxy the request with the following URI to the *GetAllItemsForOrderFunction* :
```
    /orders/{orderRef} 
```

Sample response body is:
```
[
  {
    "productId": "notebook_hardcover_blue",
    "productName": "Blue Book",
    "price": 18.9
  }
]
```

The retrieve query: 
```java
QueryRequest queryReq = new QueryRequest(TABLE_NAME);

Map<String, AttributeValue> attrValues;
attrValues.put(":v_id", new AttributeValue("ORDER-"+orderRef));

queryReq.setKeyConditionExpression("par = :v_id");
queryReq.setExpressionAttributeValues(attrValues);

List<Map<String, AttributeValue>> result = client.query(queryReq).getItems();

return result.stream()
       .map(ProductMapper::map)
       .collect(Collectors.toList());
```

### Get all the orders for a given customer

The API Gateway will proxy the request with the following URI to the *GetAllOrdersForCustomerFunction* :

```
    /customers/{orderRef}  -> getAllOrdersForCustomer
```

Sample response body is:
```
[
  {
    "orderRef": "12345",
    "customerId": "56789",
    "products": [
      {
        "productId": "notebook_hardcover_blue",
        "productName": "Blue Book",
        "price": 18.9
      }
    ]
  }
]
```

The retrieve query: 

```java
QueryRequest queryReq = new QueryRequest(TABLE_NAME);

Map<String, AttributeValue> attrValues;
attrValues.put(":v_id", new AttributeValue("CUSTOMER-"+orderRef));

queryReq.setKeyConditionExpression("par = :v_id");
queryReq.setExpressionAttributeValues(attrValues);

List<Map<String, AttributeValue>> result = client.query(queryReq).getItems();

return result.stream()
       .map(OrdersMapper::map)
       .collect(Collectors.toList());
```

When returning the response, the functions check the query result list:

| Status           |  Response Code  |
|------------------|---------------|
| Find items in DB |    200   |
| No item          |    404   |
| No path variable |    400   |
| Unexpected exception |    500   |



### Next steps
1. In the current design, the Lambda functions retrieve data from DynamoDB each time. Since the data we store will not change frequently, I would consider caching the frequent queries to improve the performance. To achieve that Amazon DynamoDB Accelerator (DAX) can be integrated to the system.

2. If the API clients are geographically dispersed, edge-optimized or regional API endpoints can be integrated. 
Edge-optimized API endpoints comes with its own CloudFront distribution managed by AWS under the hood.
Regional API endpoints would give the option to manage own CloudFront distribution.

3. The current endpoints don't support pagination, it can be implemented.

This project contains source code and supporting files for a serverless application that you can deploy with the SAM CLI. It includes the following files and folders.

- OrdersFunction/src/main - Code
 for the application's Lambda function and Project Dockerfile.
- events - Invocation events that you can use to invoke the function.
- OrdersFunction/src/test - Unit tests for the application code. 
- template.yaml - A template that defines the application's AWS resources.

The application uses several AWS resources, including Lambda functions and an API Gateway API. These resources are defined in the `template.yaml` file in this project. You can update the template to add AWS resources through the same deployment process that updates your application code.

## Deploy the sample application

The Serverless Application Model Command Line Interface (SAM CLI) is an extension of the AWS CLI that adds functionality for building and testing Lambda applications. It uses Docker to run your functions in an Amazon Linux environment that matches Lambda. It can also emulate your application's build environment and API.

To use the SAM CLI, you need the following tools.

* SAM CLI - [Install the SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* Docker - [Install Docker community edition](https://hub.docker.com/search/?type=edition&offering=community)

You may need the following for local testing.
* Java11 - [Install the Java 11](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
* Maven - [Install Maven](https://maven.apache.org/install.html)

To build and deploy your application for the first time, run the following in your shell:

```bash
sam build
sam deploy --guided
```

The first command will build a docker image from a Dockerfile and then copy the source of your application inside the Docker image. The second command will package and deploy your application to AWS, with a series of prompts:

* **Stack Name**: The name of the stack to deploy to CloudFormation. This should be unique to your account and region, and a good starting point would be something matching your project name.
* **AWS Region**: The AWS region you want to deploy your app to.
* **Confirm changes before deploy**: If set to yes, any change sets will be shown to you before execution for manual review. If set to no, the AWS SAM CLI will automatically deploy application changes.
* **Allow SAM CLI IAM role creation**: Many AWS SAM templates, including this example, create AWS IAM roles required for the AWS Lambda function(s) included to access AWS services. By default, these are scoped down to minimum required permissions. To deploy an AWS CloudFormation stack which creates or modifies IAM roles, the `CAPABILITY_IAM` value for `capabilities` must be provided. If permission isn't provided through this prompt, to deploy this example you must explicitly pass `--capabilities CAPABILITY_IAM` to the `sam deploy` command.
* **Save arguments to samconfig.toml**: If set to yes, your choices will be saved to a configuration file inside the project, so that in the future you can just re-run `sam deploy` without parameters to deploy changes to your application.

You can find your API Gateway Endpoint URL in the output values displayed after deployment.

## Use the SAM CLI to build and test locally

Build your application with the `sam build` command.

```bash
orders$ sam build
```

The SAM CLI builds a docker image from a Dockerfile and then installs dependencies defined in `OrdersFunction/pom.xml` inside the docker image. The processed template file is saved in the `.aws-sam/build` folder.

Test a single function by invoking it directly with a test event. An event is a JSON document that represents the input that the function receives from the event source. Test events are included in the `events` folder in this project.

Run functions locally and invoke them with the `sam local invoke` command.

```bash
orders$ sam local invoke OrdersFunction --event events/event.json
```

The SAM CLI can also emulate your application's API. Use the `sam local start-api` to run the API locally on port 3000.

```bash
orders$ sam local start-api
orders$ curl http://localhost:3000/
```

The SAM CLI reads the application template to determine the API's routes and the functions that they invoke. The `Events` property on each function's definition includes the route and method for each path.

```yaml
      Events:
        HelloWorld:
          Type: Api
          Properties:
            Path: /hello
            Method: get
```

## Add a resource to your application
The application template uses AWS Serverless Application Model (AWS SAM) to define application resources. AWS SAM is an extension of AWS CloudFormation with a simpler syntax for configuring common serverless application resources such as functions, triggers, and APIs. For resources not included in [the SAM specification](https://github.com/awslabs/serverless-application-model/blob/master/versions/2016-10-31.md), you can use standard [AWS CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html) resource types.

## Fetch, tail, and filter Lambda function logs

To simplify troubleshooting, SAM CLI has a command called `sam logs`. `sam logs` lets you fetch logs generated by your deployed Lambda function from the command line. In addition to printing the logs on the terminal, this command has several nifty features to help you quickly find the bug.

`NOTE`: This command works for all AWS Lambda functions; not just the ones you deploy using SAM.

```bash
orders$ sam logs -n OrdersFunction --stack-name orders --tail
```

You can find more information and examples about filtering Lambda function logs in the [SAM CLI Documentation](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-logging.html).

## Unit tests

Tests are defined in the `OrdersFunction/src/test` folder in this project.

```bash
orders$ cd OrdersFunction
OrdersFunction$ mvn test
```

## Cleanup

To delete the sample application that you created, use the AWS CLI. Assuming you used your project name for the stack name, you can run the following:

```bash
aws cloudformation delete-stack --stack-name orders
```

## Resources

See the [AWS SAM developer guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html) for an introduction to SAM specification, the SAM CLI, and serverless application concepts.

Next, you can use AWS Serverless Application Repository to deploy ready to use Apps that go beyond hello world samples and learn how authors developed their applications: [AWS Serverless Application Repository main page](https://aws.amazon.com/serverless/serverlessrepo/)
