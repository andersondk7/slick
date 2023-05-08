package org.dka.rdbms.common.dao

import org.dka.rdbms.common.model.components.ID
import org.dka.rdbms.common.model.item.Book

/**
 * adds methods beyond simple crud stuff anticipated to be mostly specific queries
 *
 * this interface is db agnostic and allows for easy unit testing since an database is not required
 */
trait BookDao extends CrudDao[Book] {}
