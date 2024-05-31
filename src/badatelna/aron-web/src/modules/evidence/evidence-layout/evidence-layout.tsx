import { useTheme } from '@material-ui/core/styles';
import classNames from 'classnames';
import { Resizable, ResizeCallback, Size } from 're-resizable';
import { default as React, useEffect, useState } from 'react';
import { Loading } from '../../../components';
import { useLayoutStyles } from '../../../styles';
// import { useSpacingStyles } from '../../../styles';
import { useStyles } from './styles';

export enum LayoutType {
  ONE_COLUMN = "ONE_COLUMN",
  TWO_COLUMN = "TWO_COLUMN",
  THREE_COLUMN = "THREE_COLUMN",
}

interface SectionRenderProps {
  layoutType: LayoutType;
}

interface EvidenceLayoutProps {
  renderTree?: (props: SectionRenderProps) => React.ReactNode;
  renderDao?: (props: SectionRenderProps) => React.ReactNode;
  renderDesc?: (props: SectionRenderProps) => React.ReactNode;
  showTree?: boolean;
  showDesc?: boolean;
  isLoading?: boolean;
}

function RoundedLineVertical(){
  return <div style={{
    background: 'currentColor', 
    height: '30px',
    width: '3px',
    borderRadius: '10px'
  }}/>
}

function RoundedLineHorizontal(){
  return <div style={{
    background: 'currentColor', 
    height: '3px',
    width: '30px',
    borderRadius: '10px'
  }}/>
}

function ResizeHandle({position}:{position?: "bottom" | "left" | "right"}){
  const classes = useStyles();
  const layoutClasses = useLayoutStyles();
  const horizontal = position !== "left" && position !== "right";

  return <div
    className={classNames(
      horizontal && classes.treeResizeHandleHorizontal,
      !horizontal && classes.treeResizeHandleVertical,
      position === "left" && classes.treeResizeHandleLeft,
      position === "right" && classes.treeResizeHandleRight,
      position === "bottom" && classes.treeResizeHandleBottom,
      layoutClasses.flexCentered
    )}
  >
    {horizontal ?
      <RoundedLineHorizontal/>:
      <RoundedLineVertical/>
  }
  </div>
}

export function EvidenceLayout({
  renderTree,
  renderDao,
  renderDesc,
  showTree,
  showDesc,
  isLoading,
}: EvidenceLayoutProps) {
  // default sizes for the tree pane
  const smTreeSize: Size = {width: "100%", height: "500px"};
  const mdTreeSize: Size = {width: "30%", height: "100%"};
  const lgTreeSize: Size = {width: "25%", height: "100%"};

  // default sizes for the item description pane
  const smDescSize: Size = {width: "100%", height: "auto"}
  const mdDescSize: Size = {width: "100%", height: "auto"}
  const lgDescSize: Size = {width: "30%", height: "100%"}

  const classes = useStyles();
  const theme = useTheme() 

  const [layoutType, setLayoutType] = useState<LayoutType>(LayoutType.ONE_COLUMN);
  const [treeSize, setTreeSize] = useState<Size>(smTreeSize);
  const [descSize, setDescSize] = useState<Size>(smDescSize);
  const [treeWidth, setTreeWidth] = useState<number | string>(mdTreeSize.width);
  const [treeHeight, setTreeHeight] = useState<number | string>(smTreeSize.height);
  const [descWidth, setDescWidth] = useState<number | string>(lgDescSize.width);


  useEffect(() => {
    const mdQuery = window.matchMedia(theme.breakpoints.up('md').replace('@media', ''));
    const lgQuery = window.matchMedia(theme.breakpoints.up('lg').replace('@media', ''));

    function handleQueryChange() {
      if(mdQuery.matches && lgQuery.matches){
        setLayoutType(LayoutType.THREE_COLUMN);
        setTreeSize({...lgTreeSize, width: treeWidth});
        setDescSize({...lgDescSize, width: descWidth});
      }
      else if(mdQuery.matches && !lgQuery.matches){
        setLayoutType(LayoutType.TWO_COLUMN);
        setTreeSize({...mdTreeSize, width: treeWidth});
        setDescSize(mdDescSize);
      } else {
        setLayoutType(LayoutType.ONE_COLUMN);
        setTreeSize({...smTreeSize, height: treeHeight});
        setDescSize(smDescSize);
      }
    }

    handleQueryChange(); // set initial pane sizes

    lgQuery.addEventListener('change',handleQueryChange);
    mdQuery.addEventListener('change', handleQueryChange);

    return function cleanup() {
      lgQuery.removeEventListener('change', handleQueryChange);
      mdQuery.removeEventListener('change', handleQueryChange);
    }
  }, [treeWidth])

  const handleResizeTree:ResizeCallback = (_event, direction, elementRef) => {
    if(direction === "right"){
      setTreeWidth(elementRef.style.width);
      setTreeSize({...treeSize, width: elementRef.style.width});
    } else if (direction === "bottom") {
      setTreeHeight(elementRef.style.height);
      setTreeSize({...treeSize, height: elementRef.style.height});
    }
  }

  const handleResizeDesc:ResizeCallback = (_event, _direction, elementRef) => {
    setDescWidth(elementRef.style.width);
    setDescSize({...descSize, width: elementRef.style.width});
  }

  return (
    <div style={{height: '100%'}}>
      <Loading {...{ loading: !!isLoading }} />
      <div className={classes.wrapperStyle}>
        {renderTree && (
          <Resizable 
            enable={{
              right: layoutType === LayoutType.THREE_COLUMN || layoutType === LayoutType.TWO_COLUMN,
              bottom: layoutType === LayoutType.ONE_COLUMN,
            }} 
            size={treeSize}
            maxWidth={layoutType !== LayoutType.ONE_COLUMN ? '50%' : undefined}
            minHeight={'50px'}
            style={{
              display: !showTree ? 'none' : undefined,
              zIndex: 10,
              marginRight: layoutType !== LayoutType.ONE_COLUMN ? '5px' : undefined,
            }}
            onResizeStop={handleResizeTree}
            handleComponent={{
              bottom: <ResizeHandle position="bottom"/>,
              right: <ResizeHandle position="right"/>,
            }}
          >
            <div className={classes.treePaneStyle}>
              {renderTree({layoutType})}
            </div>
          </Resizable>
        )}
        <div className={classNames( classes.itemDetailWrapper) }>
          {renderDao &&
            <div className={classes.daoPaneStyle}>
              {renderDao({layoutType})}
            </div>
          }
          {renderDesc && 
            <Resizable
              enable={{left: !!renderDao && layoutType === LayoutType.THREE_COLUMN}} 
              size={descSize}
              maxWidth={!!renderDao && layoutType === LayoutType.THREE_COLUMN ? '40%' : undefined}
              style={{
                display: !showDesc ? 'none' : undefined,
                overflow: 'visible',
                flexGrow: !renderDao ? 1 : undefined,
                zIndex: 10,
                marginLeft: layoutType === LayoutType.THREE_COLUMN ? '5px' : undefined,
              }}
              onResizeStop={handleResizeDesc}
              handleComponent={{
                left: <ResizeHandle position="left"/>,
              }}
            >
              <div className={classes.descPaneStyle}>
                {renderDesc({layoutType})}
              </div>
            </Resizable>
          }
        </div>
      </div>
    </div>
  );
}
