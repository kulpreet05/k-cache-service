# K CACHE SERVICE

Provide APIs for in memory and persistent caching. Cache is configured with in memory cache size and least read value is
persisted into database.

## Infrastructure Stack

- Java 21
- Spring Boot Version 3.4.1
- H2 Database
- Maven

## Build Instructions

- Configure Java 21 in environment variables
- Confgure Maven in environment variables
- Run `mvn clean install`
- If you are using POSIX based OS, like Linux, macOS, you may also use build.sh. It expects Java 21 path to be
  configured in file.
- Run `./build.sh`

## Running Instructions

- Run `java -jar target/k-cache-service-0.0.1-SNAPSHOT.jar`
- Access the application at http://localhost:5500/kCacheService/api/v1
- Access H2 Database at http://localhost:5500/kCacheService/api/h2-console
- In POSIX based OS, you may use start.sh. Run `./start.sh`

## Design Considerations

### Version 1

- Both add and get operations of cache perform in memory and database persistence operations during lock.
- Advantage of this approach, since both operations are performed in same lock, there is no chance of data inconsistency
  between in memory and database.
- Disadvantage of this approach, since database operation is blocking the thread, api throughput is reduced.

### Version 2

- Database persistence operation is done with a mirror of in memory cache.
- Sync between in memory and database is done by an independent thread, running on fixed interval.
- Both add and get operations on cache are performed in memory.
- Advantage of this approach, high throughput.
- Disadvantage of this approach, high memory consumption.

## Performance Test Results

### Version 1

#### Test Configuration 1

| Metric           | Value             |
|------------------|-------------------|
| Total Calls      | 289               |
| Total Success    | 288               |
| Total Failures   | 1                 |
| Success Rate     | 99.65%            |
| Total Iterations | 29                |
| Throughput       | 4.81 calls/second |

#### Test Configuration 2

| Metric           | Value              |
|------------------|--------------------|
| Total Calls      | 2028               |
| Total Success    | 1868               |
| Total Failures   | 160                |
| Success Rate     | 92.11%             |
| Total Iterations | 78                 |
| Average RPS      | 33.80 calls/second |

### Version 2

#### Test Configuration 1

| Metric           | Value             |
|------------------|-------------------|
| Total Calls      | 303               |
| Total Success    | 303               |
| Total Failures   | 0                 |
| Success Rate     | 100%              |
| Total Iterations | 31                |
| Throughput       | 5.05 calls/second |

#### Test Configuration 2

| Metric           | Value              |
|------------------|--------------------|
| Total Calls      | 1898               |
| Total Success    | 1898               |
| Total Failures   | 0                  |
| Success Rate     | 100%               |
| Total Iterations | 73                 |
| Average RPS      | 31.63 calls/second |