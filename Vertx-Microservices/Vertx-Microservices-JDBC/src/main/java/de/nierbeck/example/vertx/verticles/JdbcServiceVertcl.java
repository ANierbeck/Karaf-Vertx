/*
   Copyright 2016 Achim Nierbeck

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package de.nierbeck.example.vertx.verticles;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import de.nierbeck.example.vertx.encoder.BookEncoder;
import de.nierbeck.example.vertx.encoder.ListOfBookEncoder;
import de.nierbeck.example.vertx.encoder.RecipeEncoder;
import de.nierbeck.example.vertx.entity.Book;
import de.nierbeck.example.vertx.entity.Recipe;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

@Component(immediate = true, service = Verticle.class)
public class JdbcServiceVertcl extends AbstractVerticle {

    private final static Logger LOGGER = Logger.getLogger("JdbcServiceVertcl");

    @Reference
    private DataSource dataSource;

    @Reference
    private EventBus eventBus;

    private JDBCClient client;

    @Override
    public void start() throws Exception {
        super.start();
        client = JDBCClient.create(vertx, dataSource);


        eventBus.registerDefaultCodec(Book.class, new BookEncoder());
        eventBus.registerDefaultCodec(Recipe.class, new RecipeEncoder());
        eventBus.registerDefaultCodec((Class<ArrayList<Book>>) (Class<?>) ArrayList.class, new ListOfBookEncoder());

        initDb();

        MessageConsumer<Object> read = eventBus.consumer("de.nierbeck.vertx.jdbc.read"); //TODO: move those addresses to a "global" dict.
        MessageConsumer<Object> write = eventBus.consumer("de.nierbeck.vertx.jdbc.write.add");
        MessageConsumer<Object> update = eventBus.consumer("de.nierbeck.vertx.jdbc.write.update");
        MessageConsumer<Object> delete = eventBus.consumer("de.nierbeck.vertx.jdbc.delete");
        read.handler(this::handleRead);
        write.handler(this::handleWrite);
        update.handler(this::handleUpdate);
        delete.handler(this::handleDelete);
    }
    
    private void handleRead(Message<Object> message) {
        LOGGER.info("received read message: " + message.body());
        Object body = message.body();
        if (body instanceof Book) {
            Book book = (Book) body;
            Long id = book.getId();
            
            if (id != null) {
                client.getConnection(conn -> {
                    queryWithParams(conn.result(), "select * from recipe where book_id=?", new JsonArray().add(id), rs -> {
                        List<Recipe> recipes = new ArrayList<>();
                        for (JsonArray line : rs.getResults()) {
                            LOGGER.info(line.encode());
                            recipes.add( new Recipe(line.getLong(0), line.getString(1), line.getString(2), line.getLong(3)) );
                        }
                        queryWithParams(conn.result(), "select * from book where id=?", new JsonArray().add(id), result -> {
                            for(JsonArray line : result.getResults()) {
                                Book dbBook = new Book(line.getLong(0), line.getString(1), line.getString(2));
                                dbBook.setRecipes(recipes);
                                message.reply(dbBook);
                            }
                        });
                    });
                });
            } else {
                client.getConnection(conn -> {
                    query(conn.result(), "select * from book", rs -> {
                        List<Book> books = new ArrayList<>();
                        rs.getResults().stream().forEach(line -> {
                            books.add(new Book(line.getLong(0), line.getString(1), line.getString(2)));
                        });
                        message.reply((List<Book>) books);
                    });
                });
            }

        } else if (body instanceof Recipe) {
            Recipe recipe = (Recipe) body;
            Long id = recipe.getId();
            Long bookId = recipe.getBookId();
            
            if (id != null) {            
                client.getConnection(conn -> {
                    LOGGER.info("querying for recipe with id and bookid");
                    queryWithParams(conn.result(), "select * from recipe where id= ? and book_id= ?", new JsonArray().add(id).add(bookId), rs -> {
                        for (JsonArray line: rs.getResults()) {
                            recipe.setName(line.getString(1));
                            recipe.setIngredients(line.getString(2));
                            message.reply(recipe);
                        }
                    });
                });
            } else if (bookId != null) {
                client.getConnection(conn -> {
                    queryWithParams(conn.result(), "select * from recipe where book_id= ?", new JsonArray().add(bookId), rs -> {
                        List<Recipe> recipes = new ArrayList<>();
                        rs.getResults().stream().forEach(line -> {
                            recipes.add(new Recipe(line.getLong(0), line.getString(1), line.getString(2), line.getLong(3)));
                        });
                        message.reply(recipes);
                    });
                });
            }
        }
    }
    
    private void handleWrite(Message<Object> message) {
        LOGGER.info("received write message: " + message.body());
        
        if (message.body() instanceof Recipe) {
            Recipe recipe = (Recipe) message.body();
            if (recipe.getId() == null) {
                client.getConnection(conn -> {
                    startTx(conn.result(), tx -> {
                        query(conn.result(), "select max(id) from recipe", rs -> {
                            Integer newId = rs.getResults().get(0).getInteger(0) + 1;
                            updateWithParams(conn.result(), "insert into recipe values(?,?,?,?)", new JsonArray().add(newId).add(recipe.getName()).add(recipe.getIngredients()).add(recipe.getBookId()), execute -> {
                                endTx(conn.result(), txDone -> {
                                    conn.result().close(done -> {
                                        if (done.failed()) {
                                            throw new RuntimeException(done.cause());
                                        } 
                                    });
                                });
                            });
                        });
                    });
                });
            } else {
                client.getConnection(conn -> {
                    startTx(conn.result(), tx -> {
                        updateWithParams(conn.result(), "insert into recipe values(?,?,?,?)", new JsonArray().add(recipe.getId()).add(recipe.getName()).add(recipe.getIngredients()).add(recipe.getBookId()), execute -> {
                            endTx(conn.result(), txDone -> {
                                conn.result().close(done -> {
                                    if (done.failed()) {
                                        throw new RuntimeException(done.cause());
                                    }
                                });
                            });
                        });
                    });
                });
            }
        } else if (message.body() instanceof Book) {
            Book book = (Book) message.body();
            if (book.getId() == null) {
                client.getConnection(conn -> {
                    startTx(conn.result(), tx -> {
                        query(conn.result(), "select max(id) from book", rs -> {
                            Integer newId = rs.getResults().get(0).getInteger(0) + 1;
                            updateWithParams(conn.result(), "insert into book values(?,?,?)", new JsonArray().add(newId).add(book.getName()).add(book.getIsbn()), execute -> {
                                endTx(conn.result(), txDone -> {
                                    conn.result().close(done -> {
                                        if (done.failed()) {
                                            throw new RuntimeException(done.cause());
                                        }
                                    });
                                });
                            });
                        });
                    });
                });
            } else {
                client.getConnection(conn -> {
                    startTx(conn.result(), tx -> {
                        updateWithParams(conn.result(), "insert into book values(?,?,?)", new JsonArray().add(book.getId()).add(book.getName()).add(book.getIsbn()), execute -> {
                            endTx(conn.result(), txDone -> {
                                conn.result().close(done -> {
                                    if (done.failed()) {
                                        throw new RuntimeException(done.cause());
                                    }
                                });
                            });
                        });
                    });
                });
            }
        }
    }
    
    private void handleUpdate(Message<Object> message) {
        LOGGER.info("received update message: "+message.body());
        
        if (message.body() instanceof Recipe) {
            Recipe recipe = (Recipe) message.body();
            client.getConnection(conn -> {
                startTx(conn.result(), tx -> {
                    updateWithParams(conn.result(), "update recipe set name = ?, ingredients = ?, book_id = ? where id = ?", new JsonArray().add(recipe.getName()).add(recipe.getIngredients()).add(recipe.getBookId()).add(recipe.getId()), execute -> {
                        endTx(conn.result(), txDone -> {
                            conn.result().close(done -> {
                                if (done.failed()) {
                                    throw new RuntimeException(done.cause());
                                }
                            });
                        });
                    });
                });
            });
        } else if (message.body() instanceof Book) {
            Book book = (Book) message.body();
            client.getConnection(conn -> {
                startTx(conn.result(), tx -> {
                    updateWithParams(conn.result(), "update book set name = ?, isbn = ? where id = ?", new JsonArray().add(book.getName()).add(book.getIsbn()).add(book.getId()), execute -> {
                        endTx(conn.result(), txDone -> {
                            conn.result().close(done -> {
                                if (done.failed()) {
                                    throw new RuntimeException(done.cause());
                                }
                            });
                        });
                    });
                });
            });
        }
        
    }
    
    private void handleDelete(Message<Object> message) {
        LOGGER.info("received delete message: "+message.body());
        
        if (message.body() instanceof Recipe) {
            Recipe recipe = (Recipe) message.body();
            client.getConnection(conn -> {
                startTx(conn.result(), tx -> {
                    queryWithParams(conn.result(), "DELETE FROM recipe WHERE id = ? AND book_id = ?", new JsonArray().add(recipe.getId()).add(recipe.getBookId()), execute -> {
                        endTx(conn.result(), txDone -> {
                            conn.result().close(done -> {
                                if (done.failed()) {
                                    throw new RuntimeException(done.cause());
                                }
                            });
                        });
                    });
                });
            });
        } else if (message.body() instanceof Book) {
            //TODO: delete Book
        }
    }
    

    private void initDb() {
        
        LOGGER.info("initializing db");
        
        client.getConnection(conn -> {
            if (conn.failed()) {
                System.err.println(conn.cause().getMessage());
                return;
            }

            // create a test table
            execute(conn.result(), "create table book (id int primary key, name varchar(255), isbn varchar(255) )",
                    create -> {
                        execute(conn.result(),
                                "create table recipe (id int primary key, name varchar(255), ingredients varchar(2048), book_id int )",
                                create2 -> {
                                    // start a transaction
                                    startTx(conn.result(), beginTrans -> {
                                        // insert some test data
                                        execute(conn.result(),
                                                "insert into book values(1, 'Java Cookbook', '1234-56789')", insert -> {
                                                    
                                                    execute(conn.result(), "insert into recipe values(1, 'Singletons', 'Singletons are neat crispy', 1)", insertRecipe -> {
                                                    
                                                    // commit data
                                                    endTx(conn.result(), commitTrans -> {
                                                        // query some data
                                                        query(conn.result(), "select count(*) from book", rs -> {
                                                            for (JsonArray line : rs.getResults()) {
                                                                System.out.println(line.encode());
                                                            }
                                                            // and close the
                                                            // connection
                                                            conn.result().close(done -> {
                                                                if (done.failed()) {
                                                                    throw new RuntimeException(done.cause());
                                                                }
                                                            });
                                                        });
                                                    });
                                                });
                                            });
                                    });
                                });
                    });
        });
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        eventBus.unregisterDefaultCodec(Book.class);
        eventBus.unregisterDefaultCodec(Recipe.class);
        eventBus.unregisterDefaultCodec((Class<ArrayList<Book>>) (Class<?>) ArrayList.class);
        
        client.close();
    }

    private void execute(SQLConnection conn, String sql, Handler<Void> done) {
        conn.execute(sql, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }
    
    private void updateWithParams(SQLConnection conn, String sql, JsonArray params, Handler<Void> done) {
        conn.updateWithParams(sql, params, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }
            done.handle(null);
        });
    }

    private void query(SQLConnection conn, String sql, Handler<ResultSet> done) {
        conn.query(sql, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }
            done.handle(res.result());
        });
    }
    
    private void queryWithParams(SQLConnection conn, String sql, JsonArray params, Handler<ResultSet> done) {
        conn.queryWithParams(sql, params, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(res.result());
        });
    }

    private void startTx(SQLConnection conn, Handler<ResultSet> done) {
        conn.setAutoCommit(false, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }

    private void endTx(SQLConnection conn, Handler<ResultSet> done) {
        conn.commit(res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }
}
