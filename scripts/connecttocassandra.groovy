Why not create a simpler, more fluent wrapper on top of the Hector API using Groovy?
@Grab('org.hectorclient:hector-core:1.1-2')
@GrabExclude('org.apache.httpcomponents:httpcore')
import me.prettyprint.hector.api.Cluster
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.hector.api.Keyspace
import me.prettyprint.cassandra.serializers.*
import me.prettyprint.hector.api.Serializer
import me.prettyprint.hector.api.mutation.Mutator
import me.prettyprint.hector.api.ddl.*
import me.prettyprint.hector.api.beans.ColumnSlice
class Gassandra {
 def cluster
 def keyspace
 def colFamily
 Serializer serializer
 def stringSerializer = StringSerializer.get()
 private Gassandra (Keyspace keyspace) {
 this.keyspace = keyspace
 }
 Gassandra() {}
 void connect(clusterName, host, port) {
 cluster = HFactory.
 getOrCreateCluster(
 clusterName,
 "$host:$port"
 )
 }
 List<KeyspaceDefinition> getKeyspaces() {
 cluster.describeKeyspaces()
 }
 Gassandra withKeyspace(keyspaceName) {
 keyspace = HFactory.
 createKeyspace(
 keyspaceName,
 cluster
www.it-ebooks.info
Chapter 7
259
 )
 new Gassandra(keyspace)
 }
 Gassandra withColumnFamily(columnFamily, Serializer c) {
 colFamily = columnFamily
 serializer = c
 this
 }
 Gassandra insert(key, columnName, value) {
 def mutator = HFactory.
 createMutator(
 keyspace,
 serializer
 )
 def column = HFactory.
 createStringColumn(
 columnName,
 value
 )
 mutator.insert(key, colFamily, column)
 this
 }
 Gassandra insert(key, Map args) {
 def mutator = HFactory.
 createMutator(
 keyspace,
 serializer
 )
 args.each {
 mutator.insert(
 key,
 colFamily,
 HFactory.
 createStringColumn(
 it.key,
 it.value
 )
 )
 }
 this
 }
www.it-ebooks.info
Working with Databases in Groovy
260
 ColumnSlice findByKey(key) {
 def sliceQuery = HFactory.
 createSliceQuery(
 keyspace,
 serializer,
 stringSerializer,
 stringSerializer
 )
 sliceQuery.
 setColumnFamily(colFamily).
 setKey(key).
 setRange('', '', false, 100).
 execute().
 get()
 }
}
How it works...
The Gassandra class exposes a very simple, fluent interface that leverages the dynamic
nature of Groovy. The class imports the Hector API and allows writing code as follows:
def g = new Gassandra()
g.connect('test', 'localhost', '9160')
def employee = g
 .withKeyspace('hr')
 .withColumnFamily('employee', IntegerSerializer.get())
employee.insert(5005, 'name', 'Zoe')
employee.insert(5005, 'lastName', 'Ross')
employee.insert(5005, 'age', '31')
The withKeySpace and withColumnFamily methods are written in a fluent style, so that
we can pass the relevant information to Hector. Note that the withColumnFamily requires
a Serializer type to specify the type of the primary key.
The insert method accepts a Map as well, so that the previous code can be rewritten as:
employee.insert('5005',
 ['name': 'Zoe',
 'lastName': 'Ross',
 'age': '31'
 ])
www.it-ebooks.info
Chapter 7
261
To find a row by primary key, there is a findByKey method that returns a me.prettyprint.
hector.api.beans.ColumnSlice object.
println employee.findByKey(5005)
The previous statement will output:
ColumnSlice([HColumn(age=31),
 HColumn(lastName=Ross),
 HColumn(name=Zoe)])
The Gassandra class lacks many basic methods to update or delete rows and other
advanced query features. We leave them to the reader as an exercise.
See also
f http://cassandra.apache.org/
f http://hector-client.github.com/hector/build/html/index.html
