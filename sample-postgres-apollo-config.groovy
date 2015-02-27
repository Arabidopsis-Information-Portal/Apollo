grails {


    environments {
        development {
            dataSource.dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            dataSource.username = "<CHANGEME>"
            dataSource.password = "<CHANGEME>"
            dataSource.driverClassName = "org.postgresql.Driver"
            dataSource.dialect = org.hibernate.dialect.PostgresPlusDialect
            dataSource.url = "jdbc:postgresql://localhost/apollo"

            apollo.jbrowse.data.directory = "/opt/another.apollo/jbrowse/data"
        }
        test {
            dataSource.dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            dataSource.username = "<CHANGEME>"
            dataSource.password = "<CHANGEME>"
            dataSource.driverClassName = "org.postgresql.Driver"
            dataSource.dialect = org.hibernate.dialect.PostgresPlusDialect
            dataSource.url = "jdbc:postgresql://localhost/apollo-test"
            apollo.jbrowse.data.directory = "/opt/another.apollo/jbrowse/data"

        }
        production {
            dataSource.dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            dataSource.username = "<CHANGEME>"
            dataSource.password = "<CHANGEME>"
            dataSource.driverClassName = "org.postgresql.Driver"
            dataSource.dialect = org.hibernate.dialect.PostgresPlusDialect
            dataSource.url = "jdbc:postgresql://localhost/apollo-production"

            apollo.jbrowse.data.directory = "/opt/another.apollo/jbrowse/data"
            dataSource.properties {
                // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
                jmxEnabled = true
                initialSize = 5
                maxActive = 50
                minIdle = 5
                maxIdle = 25
                maxWait = 10000
                maxAge = 10 * 60000
                timeBetweenEvictionRunsMillis = 5000
                minEvictableIdleTimeMillis = 60000
                validationQuery = "SELECT 1"
                validationQueryTimeout = 3
                validationInterval = 15000
                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = false
                jdbcInterceptors = "ConnectionState"
                defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
            }

        }

    }

}

