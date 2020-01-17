package org.seekloud.theia.statistics.components

import mhtml.Var
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem

/**
  * User: XuSiRan
  * Date: 2018/12/20
  * Time: 17:05
  *
  * 基于直接页面索引的分页机制
  * 默认以5页为一组，一次请求获取一组
  * 翻页方法：指定PageInfo.page
  *
  * 参数说明：
  * T: 获取的数据类型
  * f: (第几页, 每页多少条数据)=>(获得的数据列表)
  */
class SwitchPageMod[T](var f: (Int, Int) => Future[List[T]]) {

  //每次翻页的页面重新定位位置
  private val scrollTop = 650

  private final case class PageInfo(
    var page: Int, // 1-5页
    var bigPage: Int, // 5页page为1页bigPage
    var count: Int = 50,
    var data: List[T] = Nil,
    var indexData: Var[List[T]] = Var(Nil)
  ) // 本页面page信息

  private val pageInfo = PageInfo(1, 1)

  def getIndexData: Var[List[T]] = pageInfo.indexData

  private def changePageInfo(page: Int): Unit ={
    dom.window.scrollTo(0, scrollTop)
    page match{
      case a if a > 5 =>
//        pageInfo.indexData := Nil
        f(pageInfo.bigPage + 1, pageInfo.count).map{ dataList =>
          if(dataList != Nil) {
            pageInfo.page = 1
            pageInfo.bigPage += 1
            pageInfo.data = dataList
            pageInfo.indexData := dataList.take(10)
          }
        }
      case a if a < 1 =>
//        pageInfo.indexData := Nil
        f(pageInfo.bigPage - 1, pageInfo.count).map{ dataList =>
          if(dataList != Nil && pageInfo.bigPage != 1) {
            pageInfo.page = 5
            pageInfo.bigPage -= 1
            pageInfo.data = dataList
            pageInfo.indexData := dataList.drop(40)
          }
        }
      case a =>
        val index = pageInfo.data.slice((a - 1) * 10, (a - 1) * 10 + 10)
        if(index != Nil){
          pageInfo.page = a
          pageInfo.indexData := index
        }
    }
  }

  def initPageInfo(): Unit ={
//    pageInfo.indexData := Nil
    pageInfo.data = Nil
    f(1, pageInfo.count).map{ dataList =>
      if(dataList != Nil) {
        pageInfo.page = 1
        pageInfo.bigPage = 1
        pageInfo.data = dataList
        pageInfo.indexData := dataList.take(10)
      }
      else{
        pageInfo.indexData := Nil
      }
    }
  }

  //样式需要自己写，不过类名参考的是bootstrap,可以直接使用bootstrap的css
  def pageDiv: Elem =
    <nav aria-label="Sheet Page" class="RLPageNav">
      <span style="float: left;">page
        {pageInfo.page + (pageInfo.bigPage - 1) * 5}
      </span>
      <ul class="pagination justify-content-end">
        <li class="page-item"><a class="page-link" aria-label="Previous" onclick={() => changePageInfo(page = pageInfo.page - 1)}><span>{"<"}</span></a></li>
        {for(a <- (pageInfo.bigPage - 1) * 5 + 1 to (pageInfo.bigPage - 1) * 5 + ((pageInfo.data.length - 1) / 10) + 1) yield {
        <li class="page-item" data-page={if(pageInfo.page == {if(a % 5 == 0) 5 else a % 5}) "1" else "0"}><a class="page-link" onclick={() => changePageInfo(if(a % 5 == 0) 5 else a % 5)}>{a}</a></li>
      }}
        <li class="page-item"><a class="page-link" aria-label="Next" onclick={() => changePageInfo(page = pageInfo.page + 1)}><span>{">"}</span></a></li>
      </ul>
    </nav>

}
