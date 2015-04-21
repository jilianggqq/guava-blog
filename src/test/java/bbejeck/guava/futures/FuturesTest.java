package bbejeck.guava.futures;

import bbejeck.support.model.Person;

import com.google.common.base.Function;
import com.google.common.util.concurrent.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static java.lang.System.out;

/**
 * Created by IntelliJ IDEA. User: bbejeck Date: 11/20/11 Time: 2:11 PM
 */

public class FuturesTest extends SearchingTestBase {

	private int numberTasks;
	private CountDownLatch startSignal;
	private CountDownLatch doneSignal;
	private ListeningExecutorService executorService;

	@Before
	public void setUp() throws Exception {
		numberTasks = 5;
		startSignal = new CountDownLatch(1);
		doneSignal = new CountDownLatch(numberTasks);
		// Executors.ne
		executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
	}

	@After
	public void tearDown() {
		executorService.shutdownNow();
	}

	// @Test
	// public void testChainSearchSettableFuture() throws Exception {
	//
	// Function<List<String>, ListenableFuture<List<Person>>> queryFunction =
	// new Function<List<String>, ListenableFuture<List<Person>>>() {
	// @Override
	// public ListenableFuture<List<Person>> apply(final List<String> ids) {
	// SettableFuture<List<Person>> sf = SettableFuture.create();
	// sf.set(dbService.getPersonsById(ids));
	// return sf;
	// }
	// };
	//
	// ListenableFuture<List<String>> indexSearch =
	// luceneSearcher.searchAsync("firstName:martin");
	//
	// ListenableFuture<List<Person>> results = Futures.transform(indexSearch,
	// queryFunction);
	// List<Person> persons = results.get(1, TimeUnit.SECONDS);
	// assertThat(persons.size(), is(74));
	// for (Person person : persons) {
	// assertThat(person.firstName, is("Martin"));
	// }
	// }

	@Test
	public void testChainSearchFunction() throws Exception {

		AsyncFunction<List<String>, List<Person>> queryFunction = new AsyncFunction<List<String>, List<Person>>() {
			@Override
			public ListenableFuture<List<Person>> apply(final List<String> ids) {
				return dbService.getPersonsByIdAsync(ids);
			}
		};

		ListenableFuture<List<String>> indexSearch = luceneSearcher.searchAsync("firstName:martin");

		ListenableFuture<List<Person>> results = Futures.transform(indexSearch, queryFunction, executorService);
		List<Person> persons = results.get(1, TimeUnit.SECONDS);
		assertThat(persons.size(), is(74));
		for (Person person : persons) {
			assertThat(person.firstName, is("Martin"));
		}
	}

	@Test
	public void testTransformSearch() throws Exception {

		Function<List<String>, List<Person>> transformSearchResults = new Function<List<String>, List<Person>>() {
			@Override
			public List<Person> apply(List<String> ids) {
				out.println(Thread.currentThread().getName());
				return dbService.getPersonsById(ids);
			}
		};

		ListenableFuture<List<String>> indexSearch = luceneSearcher.searchAsync("firstName:martin");
		ListenableFuture<List<Person>> transformedResults = Futures.transform(indexSearch, transformSearchResults, executorService);

		List<Person> persons = transformedResults.get(1, TimeUnit.SECONDS);
		int expectedSize = 74;
		assertThat(persons.size(), is(expectedSize));
		for (Person person : persons) {
			assertThat(person.firstName, is("Martin"));
		}
	}

	@Test
	public void allAsListSuccess() throws Exception {
		ListenableFuture<List<Person>> lf1 = getPersonsByFirstNameFuture("martin", false);
		ListenableFuture<List<Person>> lf2 = getPersonsByFirstNameFuture("bob", false);
		ListenableFuture<List<Person>> lf3 = getPersonsByFirstNameFuture("emily", false);
		ListenableFuture<List<Person>> lf4 = getPersonsByFirstNameFuture("mona", false);
		ListenableFuture<List<Person>> lf5 = getPersonsByFirstNameFuture("tom", false);

		ListenableFuture<List<List<Person>>> lfResults = Futures.allAsList(lf1, lf2, lf3, lf4, lf5);
		out.println(startSignal.getCount());
		startSignal.countDown();
		// startSignal.get
		List<List<Person>> listOfPersonLists = lfResults.get();
		assertThat(listOfPersonLists.size() > 0, is(true));
		for (List<Person> personList : listOfPersonLists) {
			assertThat(personList.size() > 0, is(true));
		}
	}

	// @Test(expected = ExecutionException.class)
	// public void allAsListSuccessOneFailure() throws Exception {
	// ListenableFuture<List<Person>> lf1 =
	// getPersonsByFirstNameFuture("martin", false);
	// ListenableFuture<List<Person>> lf2 = getPersonsByFirstNameFuture("bob",
	// false);
	// ListenableFuture<List<Person>> lf3 = getPersonsByFirstNameFuture("emily",
	// true);
	// ListenableFuture<List<Person>> lf4 = getPersonsByFirstNameFuture("mona",
	// false);
	// ListenableFuture<List<Person>> lf5 = getPersonsByFirstNameFuture("tom",
	// false);
	//
	// ListenableFuture<List<List<Person>>> lfResults = Futures.allAsList(lf1,
	// lf2, lf3, lf4, lf5);
	// startSignal.countDown();
	// List<List<Person>> listOfPersonLists = lfResults.get();
	// fail("should not get here");
	// }

	@Test
	public void successfulAsListSuccessOneFailure() throws Exception {
		ListenableFuture<List<Person>> lf1 = getPersonsByFirstNameFuture("martin", true);
		ListenableFuture<List<Person>> lf2 = getPersonsByFirstNameFuture("bob", false);
		ListenableFuture<List<Person>> lf3 = getPersonsByFirstNameFuture("emily", true);
		ListenableFuture<List<Person>> lf4 = getPersonsByFirstNameFuture("mona", false);
		ListenableFuture<List<Person>> lf5 = getPersonsByFirstNameFuture("tom", false);

		ListenableFuture<List<List<Person>>> lfResults = Futures.successfulAsList(lf1, lf2, lf3, lf4, lf5);
		startSignal.countDown();


		Futures.addCallback(lfResults, new FutureCallback<List<List<Person>>>() {
			@Override
			public void onSuccess(List<List<Person>> result) {
				// TODO Auto-generated method stub
				out.println("ok");
				out.println("size:" + result.size());
				for (int i = 0; i < result.size(); i++) {
					List<Person> list = result.get(i);
					if (list != null) {
						for (Person person : list) {
							out.print(person.email + " ");
						}
						out.println();
					} else {
						out.println("index " + i + " is null");
					}
				}

			}

			@Override
			public void onFailure(Throwable t) {
				// TODO Auto-generated method stub
				out.println("error");

			}
		});

		// ����Ҫʹ��get�����������޷��õ�ֵ��
		/* List<List<Person>> listOfPersonLists = */lfResults.get();
		// assertThat(listOfPersonLists.size() == 5, is(true));
		//
		// // have null values failed
		// assertThat(listOfPersonLists.get(0), is(nullValue()));
		// assertThat(listOfPersonLists.get(2), is(nullValue()));
		//
		// // succeeded returned valid results
		// assertThat(listOfPersonLists.get(1).size() > 0, is(true));
		// assertThat(listOfPersonLists.get(3).size() > 0, is(true));
		// assertThat(listOfPersonLists.get(4).size() > 0, is(true));
		out.println("end");
	}

	private ListenableFuture<List<Person>> getPersonsByFirstNameFuture(final String firstName, final boolean error) {
		return executorService.submit(new Callable<List<Person>>() {
			@Override
			public List<Person> call() throws Exception {
				out.println(Thread.currentThread().getName() + "--" + startSignal.getCount());
				startSignal.await();
				out.println(Thread.currentThread().getName() + "--" + startSignal.getCount());
				if (error) {
					throw new RuntimeException("Ooops!");
				}
				List<String> ids = luceneSearcher.search("firstName:" + firstName);
				List<Person> persons = dbService.getPersonsById(ids);
				return persons;
			}
		});
	}

	private Callable<String> getSimpleCallable(final String label) {
		return new Callable<String>() {
			@Override
			public String call() throws Exception {
				return label;
			}
		};
	}
}
